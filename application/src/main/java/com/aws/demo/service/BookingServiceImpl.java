package com.aws.demo.service;

import com.aws.demo.amazons3.AmazonS3ResourceStorage;
import com.aws.demo.constants.PhotoConstants;
import com.aws.demo.constants.StatusCodeConstants;
import com.aws.demo.data.dto.BookingDto;
import com.aws.demo.data.entity.PhotoEntity;
import com.aws.demo.data.entity.ReservationEntity;
import com.aws.demo.data.repository.BookingRepository;
import com.aws.demo.data.repository.PhotoRepository;
import com.aws.demo.rekognition.CompareFaces;
import com.aws.demo.utils.DtoUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class BookingServiceImpl implements BookingService {

    BookingRepository bookingRepository;

    PhotoRepository photoRepository;
    CompareFaces compareFaces;

    AmazonS3ResourceStorage amazonS3ResourceStorage;

    @Autowired
    public BookingServiceImpl(BookingRepository bookingRepository, PhotoRepository photoRepository, CompareFaces compareFaces, AmazonS3ResourceStorage amazonS3ResourceStorage) {
        this.bookingRepository = bookingRepository;
        this.photoRepository = photoRepository;
        this.compareFaces = compareFaces;
        this.amazonS3ResourceStorage = amazonS3ResourceStorage;
    }

    @Override
    @Transactional
    public BookingDto createReserve(BookingDto bookingDto) {
        bookingDto.setBookingId(UUID.randomUUID().toString());
        ReservationEntity reservationEntity;

        log.info("booking is started");

        reservationEntity = DtoUtil.convertToReserveEntity(bookingDto);
        bookingRepository.save(reservationEntity);

        BookingDto returnDto = DtoUtil.convertToReserveDto(reservationEntity);

        log.info("booking is completed");

        return returnDto;
    }

    @Override
    public BookingDto getReserveByBookingId(String bookingId) {
        ReservationEntity reservationEntity = bookingRepository.findByBookingId(bookingId);
        BookingDto bookingDto = DtoUtil.convertToReserveDto(reservationEntity);

        bookingDto.setPassword(DtoUtil.makePassword());

        return bookingDto;
    }

    @Override
    @Transactional
    public BookingDto uploadPhoto(String bookingId, MultipartFile multipartFile) {
        ReservationEntity reservationEntity = bookingRepository.findByBookingId(bookingId);

        if (reservationEntity == null) {
            BookingDto returnDto = DtoUtil.convertToReserveDto(new ReservationEntity());
            returnDto.setErrCode(StatusCodeConstants.serverErrorCodeStorageUploadFail);
            returnDto.setErrMsg(StatusCodeConstants.serverErrorDescStorageUploadFail);
            return returnDto;
        }

        log.info("uploadPhoto - start bId : " + bookingId);

        // 1. Store Image file to S3
        String fileName = storeImg(multipartFile, PhotoConstants.PhotoType.REGISTER);
        log.info("uploadPhoto - s3 upload complete");

        if (TextUtils.isEmpty(fileName)) {
            BookingDto returnDto = DtoUtil.convertToReserveDto(new ReservationEntity());
            returnDto.setErrCode(StatusCodeConstants.serverErrorCodeStorageUploadFail);
            returnDto.setErrMsg(StatusCodeConstants.serverErrorDescStorageUploadFail);
            return returnDto;
        }

        log.info("uploadPhoto - fileName : " + fileName);

        // 2. Check Face Detect
        boolean isDetectFaces = compareFaces.detectFacesinImage(fileName);
        log.info("uploadPhoto - isDetectFaces : " + isDetectFaces);

        if (!isDetectFaces) {
            BookingDto returnDto = DtoUtil.convertToReserveDto(new ReservationEntity());
            returnDto.setErrCode(StatusCodeConstants.badRequestCodeInvalidParam);
            returnDto.setErrMsg(StatusCodeConstants.badRequestDescInvalidParam);

            return returnDto;
        }

        // 2-1. Check for Covered Mask
//        boolean isDetectMask = compareFaces.detectPPE(fileName);
//        log.info("uploadPhoto - isDetectMask : " + isDetectMask);
//
//        if (isDetectMask) {
//            BookingDto returnDto = DtoUtil.convertToReserveDto(new ReservationEntity());
//            returnDto.setErrCode(StatusCodeConstants.badRequestCodeCoveredMask);
//            returnDto.setErrMsg(StatusCodeConstants.badRequestDescCoveredMask);
//
//            return returnDto;
//        }

        // 3. Update DB - photo Img
        reservationEntity.setPhotoImg(fileName);
        bookingRepository.save(reservationEntity);

        PhotoEntity photoEntity = new PhotoEntity();
        photoEntity.setBookingId(bookingId);
        photoEntity.setCheckIn(0);
        photoEntity.setPhotoImg(fileName);
        photoRepository.save(photoEntity);

        log.info("uploadPhoto - db update complete");

        BookingDto returnDto = DtoUtil.convertToReserveDto(reservationEntity);

        log.info("uploadPhoto - fully complete");

        return returnDto;
    }

    @Override
    public String storeImg(MultipartFile multipartFile, PhotoConstants.PhotoType photoType) {
        String fileName = amazonS3ResourceStorage.store(multipartFile);

        return fileName;
    }

    @Override
    @Transactional
    public BookingDto checkIn(MultipartFile multipartFile) {
        // 1. Check-In - Upload Photo
        String fileName = storeImg(multipartFile, PhotoConstants.PhotoType.CHECKIN);
        log.info("checkIn - sourceFile : " + fileName);
        if (TextUtils.isEmpty(fileName)) {
            BookingDto returnDto = DtoUtil.convertToReserveDto(new ReservationEntity());
            returnDto.setErrCode(StatusCodeConstants.serverErrorCodeStorageUploadFail);
            returnDto.setErrMsg(StatusCodeConstants.serverErrorDescStorageUploadFail);
            return returnDto;
        }

        // 2. Check Face Detect
        boolean isDetectFaces = compareFaces.detectFacesinImage(fileName);
        log.info("uploadPhoto - isDetectFaces : " + isDetectFaces);

        if (!isDetectFaces) {
            BookingDto returnDto = DtoUtil.convertToReserveDto(new ReservationEntity());
            returnDto.setErrCode(StatusCodeConstants.badRequestCodeInvalidParam);
            returnDto.setErrMsg(StatusCodeConstants.badRequestDescInvalidParam);
            return returnDto;
        }

        // 2-1. Check for Covered Mask
/*
        boolean isDetectMask = compareFaces.detectPPE(fileName);
        log.info("uploadPhoto - isDetectMask : " + isDetectMask);

        if (isDetectMask) {
            BookingDto returnDto = DtoUtil.convertToReserveDto(new ReservationEntity());
            returnDto.setErrCode(StatusCodeConstants.badRequestCodeCoveredMask);
            returnDto.setErrMsg(StatusCodeConstants.badRequestDescCoveredMask);

            return returnDto;
        }
*/

        //3. Compare Face
        Float faceSimilarity = 0F;

        List<PhotoEntity> photoList = photoRepository.findPhotoTop50();
        String resultBookingId = "";
        String resultPhotoImg = "";

        for (PhotoEntity entity : photoList) {
            resultPhotoImg = entity.getPhotoImg();
            log.info("checkIn - targetFile : " + resultPhotoImg);
            faceSimilarity = compareFaces.compareFace(fileName, resultPhotoImg);

            if (faceSimilarity > 80F) {
                log.info("checkIn - success");
                resultBookingId = entity.getBookingId();
                break;
            }
        }

        if ((faceSimilarity < 80F) || TextUtils.isEmpty(resultBookingId)) {
            log.info("checkIn Fail!");
            BookingDto returnDto = DtoUtil.convertToReserveDto(new ReservationEntity());
            returnDto.setErrCode(StatusCodeConstants.serverErrorCodeRekognitionMatchFail);
            returnDto.setErrMsg(StatusCodeConstants.serverErrorDescRekognitionMatchFail);
            return returnDto;
        }

        // 3. Query/Update from Reservation DB
        ReservationEntity reservationEntity = bookingRepository.findByBookingId(resultBookingId);
        reservationEntity.setSimilarity(faceSimilarity);
        bookingRepository.save(reservationEntity);

        BookingDto bookingDto = DtoUtil.convertToReserveDto(reservationEntity);

        // 4. Update to Photo DB
        try {
            PhotoEntity photoEntity = photoRepository.findByBookingId(resultBookingId);
            photoEntity.setCheckIn(1);
            photoRepository.save(photoEntity);
        } catch (Exception e) {
            log.error("checkIn - Occurred Exception!!");
            BookingDto returnDto = DtoUtil.convertToReserveDto(new ReservationEntity());
            returnDto.setErrCode(StatusCodeConstants.serverErrorCodeDatabaseFail);
            returnDto.setErrMsg(StatusCodeConstants.serverErrorDescDatabaseFail);
            return returnDto;
        }

        return bookingDto;
    }

    @Override
    public byte[] getPhoto(String filename) throws IOException {
        byte[] photoImg = amazonS3ResourceStorage.getFile(filename);

        return photoImg;

    }

    @Override
    public boolean checkMask(MultipartFile multipartFile) {
        return false;
    }
/*
    @Override
    public boolean checkMask(MultipartFile multipartFile) {
        // 1. Check-In - Upload Photo
        String fileName = storeImg(multipartFile, PhotoConstants.PhotoType.REGISTER);
        log.info("checkMask - sourceFile : " + fileName);
        if (TextUtils.isEmpty(fileName)) {
            BookingDto returnDto = DtoUtil.convertToReserveDto(new ReservationEntity());
            returnDto.setErrCode(StatusCodeConstants.serverErrorCodeStorageUploadFail);
            returnDto.setErrMsg(StatusCodeConstants.serverErrorDescStorageUploadFail);
            return returnDto;
        }

        // 2. Check Mask Detect
        boolean isDetectPPE = compareFaces.detectPPE(fileName);
        log.info("testPPE - isDetectPPE : " + isDetectPPE);

        if (!isDetectFaces) {
            BookingDto returnDto = DtoUtil.convertToReserveDto(new ReservationEntity());
            returnDto.setErrCode(StatusCodeConstants.badRequestCodeInvalidParam);
            returnDto.setErrMsg(StatusCodeConstants.badRequestDescInvalidParam);
            return returnDto;
        }
        return true;
    }

 */
}