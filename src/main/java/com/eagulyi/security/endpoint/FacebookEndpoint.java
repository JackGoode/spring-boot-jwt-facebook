package com.eagulyi.security.endpoint;

import com.eagulyi.common.ErrorCode;
import com.eagulyi.common.ErrorResponse;
import com.eagulyi.common.WebUtil;
import com.eagulyi.security.auth.jwt.JwtAuthenticationToken;
import com.eagulyi.security.config.WebSecurityConfig;
import com.eagulyi.security.model.UserContext;
import com.eagulyi.security.model.token.JwtTokenFactory;
import com.eagulyi.user.entity.User;
import com.eagulyi.user.model.json.facebook.FacebookUserData;
import com.eagulyi.user.service.FacebookService;
import com.eagulyi.user.service.UserServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.io.IOException;

/**
 * Created by eugene on 4/14/17.
 */
@RestController
@RequestMapping("/api/fb")
public class FacebookEndpoint {
    private final JwtTokenFactory tokenFactory;
    private final UserServiceImpl userService;
    private final WebUtil webUtil;
    private final FacebookService facebookService;

    @Autowired
    public FacebookEndpoint(JwtTokenFactory tokenFactory,
                            UserServiceImpl userService,
                            WebUtil webUtil,
                            FacebookService facebookService) {
        this.tokenFactory = tokenFactory;
        this.userService = userService;
        this.webUtil = webUtil;
        this.facebookService = facebookService;
    }

    private static final Logger LOG = LoggerFactory.getLogger(FacebookEndpoint.class);

    @RequestMapping(value = "/login", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    @Transactional
    public void facebookLogin(HttpServletRequest request, HttpServletResponse response) {
        try {
            UserContext userContext = facebookService.processFbToken(request.getHeader(WebSecurityConfig.FB_TOKEN_HEADER_PARAM));
            response.setStatus(HttpStatus.OK.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            webUtil.writeServletResponse(response, tokenFactory.createTokenPair(userContext));
        } catch (AuthenticationException e) {
            LOG.error(e.getMessage());
            webUtil.writeServletResponse(response, ErrorResponse.of("Cannot authenticate", ErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
        }
    }

    @RequestMapping(value = "/data", method = RequestMethod.GET)
    public @ResponseBody
    FacebookUserData getUserFbData(JwtAuthenticationToken token, HttpServletResponse response) throws IOException {
        UserContext userContext = (UserContext) token.getPrincipal();
        User user = userService.getByUsername(userContext.getUsername()).get(); // TODO handle optional
        return facebookService.getFbData(user.getUsername());
    }


}