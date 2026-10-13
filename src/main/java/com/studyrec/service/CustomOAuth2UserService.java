package com.studyrec.service;

import com.studyrec.model.User;
import com.studyrec.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = super.loadUser(userRequest);
        
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String profileImage = oauthUser.getAttribute("picture");
        
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setProfileImage(profileImage);
            userRepository.save(newUser);
        } else {
            User existingUser = userOptional.get();
            if(profileImage != null && !profileImage.equals(existingUser.getProfileImage())) {
                existingUser.setProfileImage(profileImage);
            }
            if(name != null && !name.equals(existingUser.getName())) {
                existingUser.setName(name);
            }
            userRepository.save(existingUser);
        }

        return oauthUser;
    }
}
