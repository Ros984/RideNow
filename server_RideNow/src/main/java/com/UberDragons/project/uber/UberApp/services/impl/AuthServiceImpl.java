package com.UberDragons.project.uber.UberApp.services.impl;

import com.UberDragons.project.uber.UberApp.dto.DriverDto;
import com.UberDragons.project.uber.UberApp.dto.SignupDto;
import com.UberDragons.project.uber.UberApp.dto.UserDto;
import com.UberDragons.project.uber.UberApp.entities.Driver;
import com.UberDragons.project.uber.UberApp.entities.User;
import com.UberDragons.project.uber.UberApp.entities.enums.Role;
import com.UberDragons.project.uber.UberApp.exception.ResourceNotFoundException;
import com.UberDragons.project.uber.UberApp.exception.RuntimeConflictException;
import com.UberDragons.project.uber.UberApp.repositories.UserRepository;
import com.UberDragons.project.uber.UberApp.security.JWTService;
import com.UberDragons.project.uber.UberApp.services.AuthService;
import com.UberDragons.project.uber.UberApp.services.DriverService;
import com.UberDragons.project.uber.UberApp.services.RiderService;
import com.UberDragons.project.uber.UberApp.services.WalletService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

import static com.UberDragons.project.uber.UberApp.entities.enums.Role.DRIVER;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final RiderService riderService;
    private final WalletService walletService;
    private final DriverService driverService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;

    @Override
    public String[] login(String email, String password) {
        System.out.println("🔐 AuthService: Attempting login for email: " + email);
        
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        User user = (User) authentication.getPrincipal();
        System.out.println("✅ AuthService: Authentication successful for user: " + user.getEmail());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        System.out.println("✅ AuthService: Tokens generated successfully");
        return new String[]{accessToken, refreshToken};
    }

    @Override
    @Transactional
    public UserDto signup(SignupDto signupDto) {
        System.out.println("📝 AuthService: Attempting signup for email: " + signupDto.getEmail());
        
        User user = userRepository.findByEmail(signupDto.getEmail()).orElse(null);
        if(user != null)
            throw new RuntimeConflictException("Cannot signup, User already exists with email "+signupDto.getEmail());

        User mappedUser = modelMapper.map(signupDto, User.class);
        
        // Convert string roles to Role enum
        Set<Role> roles = signupDto.getRoles().stream()
            .map(Role::valueOf)
            .collect(Collectors.toSet());
        mappedUser.setRoles(roles);
        
        mappedUser.setPassword(passwordEncoder.encode(mappedUser.getPassword()));
        User savedUser = userRepository.save(mappedUser);

        System.out.println("✅ AuthService: User saved with ID: " + savedUser.getId());

        // Create user related entities
        if (savedUser.getRoles().contains(Role.RIDER)) {
            riderService.createNewRider(savedUser);
            System.out.println("✅ AuthService: Rider profile created");
        }
        walletService.createNewWallet(savedUser);
        System.out.println("✅ AuthService: Wallet created");

        UserDto userDto = modelMapper.map(savedUser, UserDto.class);
        // Convert Role enum to strings
        userDto.setRoles(savedUser.getRoles().stream()
            .map(Role::name)
            .collect(Collectors.toSet()));
        
        return userDto;
    }

    @Override
    public UserDto findByEmail(String email) {
        System.out.println("🔍 AuthService: Finding user by email: " + email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        UserDto userDto = modelMapper.map(user, UserDto.class);
        // Convert Role enum to strings
        userDto.setRoles(user.getRoles().stream()
            .map(Role::name)
            .collect(Collectors.toSet()));
        
        System.out.println("✅ AuthService: User found with roles: " + userDto.getRoles());
        return userDto;
    }

    @Override
    public DriverDto onboardNewDriver(Long userId, String vehicleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id "+userId));

        if(user.getRoles().contains(DRIVER))
            throw new RuntimeConflictException("User with id "+userId+" is already a Driver");

        Driver createDriver = Driver.builder()
                .user(user)
                .rating(0.0)
                .vehicleId(vehicleId)
                .available(true)
                .build();
        user.getRoles().add(DRIVER);
        userRepository.save(user);
        Driver savedDriver = driverService.createNewDriver(createDriver);
        return modelMapper.map(savedDriver, DriverDto.class);
    }

    @Override
    public String refreshToken(String refreshToken) {
        Long userId = jwtService.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found " +
                "with id: "+userId));

        return jwtService.generateAccessToken(user);
    }
}