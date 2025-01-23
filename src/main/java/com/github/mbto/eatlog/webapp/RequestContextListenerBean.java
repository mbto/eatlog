package com.github.mbto.eatlog.webapp;

import jakarta.servlet.annotation.WebListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextListener;

@Configuration
@WebListener
public class RequestContextListenerBean extends RequestContextListener {
}