package com.aws.demo.data.repository;

import com.aws.demo.data.entity.ReservationEntity;

public interface BookingRepository {
    ReservationEntity findByBookingId(String bookingId);

    ReservationEntity save(ReservationEntity reservationEntity);

    void delete(ReservationEntity reservationEntity);

}
