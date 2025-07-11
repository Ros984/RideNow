export interface PointDto {
  coordinates: [number, number]; // [longitude, latitude]
  type: string;
}

export interface UserDto {
  id: string;
  name: string;
  email: string;
  phoneNumber: string;
  roles: string[];
}

export interface RiderDto {
  id: string;
  user: UserDto;
  rating: number;
}

export interface DriverDto {
  id: string;
  user: UserDto;
  rating: number;
  vehicleId?: string;
  available: boolean;
}

export interface RideRequestDto {
  id: string;
  pickupLocation: PointDto;
  dropOffLocation: PointDto;
  paymentMethod: 'CASH' | 'WALLET';
  fare: number;
  rideRequestStatus: 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED';
  rider: RiderDto;
  driver?: DriverDto;
  createdTime: string;
  otp?: string;
}

export interface LoginResponseDto {
  accessToken: string;
  refreshToken: string;
  user: UserDto;
}

export interface ApiError {
  message: string;
  statusCode: number;
}

export interface PageRideDto {
  content: RideRequestDto[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}

export interface RideStartDto {
  otp: string;
}

export interface SignupDto {
  name: string;
  email: string;
  phoneNumber: string;
  password: string;
  roles: string[];
}

export interface LoginDto {
  email: string;
  password: string;
}

export interface RideBookingDto {
  pickupLocation: PointDto;
  dropOffLocation: PointDto;
  paymentMethod: 'CASH' | 'WALLET';
}

export interface RatingDto {
  rating: number;
  feedback?: string;
}