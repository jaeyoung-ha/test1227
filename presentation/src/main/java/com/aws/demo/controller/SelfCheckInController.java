package com.aws.demo.controller;

import com.aws.demo.constants.StatusCodeConstants;
import com.aws.demo.data.dto.BookingDto;
import com.aws.demo.data.dto.CommonReturnDto;
import com.aws.demo.service.BookingService;
import com.aws.demo.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@Slf4j
@RequestMapping("/demo-service")
public class SelfCheckInController {

    private Environment env;
    BookingService bookingService;

    @Autowired
    public SelfCheckInController(Environment env, BookingService bookingService) {
        this.env = env;
        this.bookingService = bookingService;
    }

    @PostMapping("/booking")
    public ResponseEntity<CommonReturnDto<ResponseBooking>> createReserve(@RequestBody RequestBooking bookingDetails) {

        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        BookingDto bookingDto = mapper.map(bookingDetails, BookingDto.class);
        bookingService.createReserve(bookingDto);

        ResponseBooking responseBooking = mapper.map(bookingDto, ResponseBooking.class);

        return new ResponseEntity<>(
                CommonReturnDto.<ResponseBooking>builder()
                        .statusCode(TextUtils.isEmpty(bookingDto.getErrCode()) ? StatusCodeConstants.okCodeRequestSuccess : bookingDto.getErrCode())
                        .statusMsg(TextUtils.isEmpty(bookingDto.getErrMsg()) ? StatusCodeConstants.okDescRequestSuccess : bookingDto.getErrMsg())
                        .data(responseBooking)
                        .build(),
                HttpStatus.OK);
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<CommonReturnDto<ResponseGetInfo>> getReserve(@PathVariable("bookingId") String bookingId) {
        BookingDto bookingDto = bookingService.getReserveByBookingId(bookingId);
        ResponseGetInfo responseGetInfo = new ModelMapper().map(bookingDto, ResponseGetInfo.class);

        return new ResponseEntity<>(
                CommonReturnDto.<ResponseGetInfo>builder()
                        .statusCode(TextUtils.isEmpty(bookingDto.getErrCode()) ? StatusCodeConstants.okCodeRequestSuccess : bookingDto.getErrCode())
                        .statusMsg(TextUtils.isEmpty(bookingDto.getErrMsg()) ? StatusCodeConstants.okDescRequestSuccess : bookingDto.getErrMsg())
                        .data(responseGetInfo)
                        .build(),
                HttpStatus.OK);
    }

    @PostMapping("/booking/photo/{bookingId}")
    public ResponseEntity<CommonReturnDto<ResponseUploadPhoto>> uploadPhoto(@PathVariable("bookingId") String bookingId, @RequestPart("file") MultipartFile multipartFile) {
        BookingDto bookingDto = bookingService.uploadPhoto(bookingId, multipartFile);
        ResponseUploadPhoto result = new ModelMapper().map(bookingDto, ResponseUploadPhoto.class);

        return new ResponseEntity<>(
                CommonReturnDto.<ResponseUploadPhoto>builder()
                        .statusCode(TextUtils.isEmpty(bookingDto.getErrCode()) ? StatusCodeConstants.okCodeRequestSuccess : bookingDto.getErrCode())
                        .statusMsg(TextUtils.isEmpty(bookingDto.getErrMsg()) ? StatusCodeConstants.okDescRequestSuccess : bookingDto.getErrMsg())
                        .data(result)
                        .build(),
                HttpStatus.OK);
    }

    @PostMapping("/checkin")
    public ResponseEntity<CommonReturnDto<ResponseCheckIn>> checkIn(@RequestPart("file") MultipartFile multipartFile) {
        BookingDto bookingDto = bookingService.checkIn(multipartFile);
        ResponseCheckIn result = new ModelMapper().map(bookingDto, ResponseCheckIn.class);

        return new ResponseEntity<>(
                CommonReturnDto.<ResponseCheckIn>builder()
                        .statusCode(TextUtils.isEmpty(bookingDto.getErrCode()) ? StatusCodeConstants.okCodeRequestSuccess : bookingDto.getErrCode())
                        .statusMsg(TextUtils.isEmpty(bookingDto.getErrMsg()) ? StatusCodeConstants.okDescRequestSuccess : bookingDto.getErrMsg())
                        .data(result)
                        .build(),
                HttpStatus.OK);
    }

    @GetMapping("/getPhoto/{filename}")
    public ResponseEntity<byte[]> getPhoto(@PathVariable("filename") String filename) {
        log.info("/getPhoto - filename : " + filename);

        byte[] photoImg = new byte[0];

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);

        if (TextUtils.isEmpty(filename) || "undefined".equals(filename)) {
            return new ResponseEntity<>(photoImg, headers, HttpStatus.OK);
        }

        try {
            photoImg = bookingService.getPhoto(filename);
        } catch (IOException e) {
            log.error("/getPhoto occurred IOException");
        }

        return new ResponseEntity<>(photoImg, headers, HttpStatus.OK);
    }
}
