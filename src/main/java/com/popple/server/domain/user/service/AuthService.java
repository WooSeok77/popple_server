package com.popple.server.domain.user.service;

import com.popple.server.domain.entity.RegisterToken;
import com.popple.server.domain.entity.Member;
import com.popple.server.domain.user.dto.*;
import com.popple.server.domain.user.vo.Token;
import com.popple.server.domain.user.vo.TokenPayload;
import com.popple.server.domain.user.vo.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {
    private final MemberService memberService;
    private final SellerService sellerService;
    private final EmailService emailService;
    private final RegisterTokenService registerTokenService;
    private final TokenService tokenService;


    public void checkProceedEmail(String email) {
        memberService.checkExistProceed(email);
    }

    public void generateRegisterTokenAndSendEmail(String email) {

        RegisterToken generateToken = registerTokenService.generateToken(email);
        EmailSource emailSource = emailService.getEmailSource(email);

        emailService.sendMail(emailSource, generateToken.getRegisterToken());
    }


    public CreateUserResponseDto register(CreateUserRequestDto createUserRequestDto) {
        CreateUserResponseDto createUserResponseDto = memberService.createWithPassword(createUserRequestDto);
        generateRegisterTokenAndSendEmail(createUserResponseDto.getEmail());

        return createUserResponseDto;
    }

    public String verifyRegisterToken(String email, String registerToken) {
        return registerTokenService.verifyToken(email, registerToken);
    }

    public void checkDuplicationNicknameAndEmail(String nickname, String email, Role role) {
        if (nickname == null && email == null) {
            throw new RuntimeException("올바르지 않은 요청입니다.");
        }

        if (role.equals(Role.SELLER)) {
            // TODO Seller 완성하고 구현하기
//            sellerService.checkDuplication(nickname, email);
            return;
        }

        memberService.checkDuplication(nickname, email);
    }

    public Token generateAccessAndRefreshToken(String email) {

        Member member = memberService.getUser(email);
        TokenPayload tokenPayload = member.toPayload();
        String accessToken = tokenService.generateAccessToken(tokenPayload);
        String refreshToken = tokenService.generateRefreshToken(tokenPayload);

        return Token.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

    }

    public LoginResponseDto login(String email, String password, Role role) {

        if (role.equals(Role.USER)) {

            registerTokenService.checkRegisteredEmail(email);

            Member member = memberService.getUser(email, password);

            TokenPayload tokenPayload = member.toPayload();
            String accessToken = tokenService.generateAccessToken(tokenPayload);
            String refreshToken = tokenService.generateRefreshToken(tokenPayload);

            return LoginResponseDto.builder()
                    .userId(member.getId())
                    .email(member.getEmail())
                    .profileImgUrl(member.getProfileImgUrl())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        }
        //TODO Seller 로그인 구현 후 User 로그인과 하나로 합칠 것
        return null;
    }
}
