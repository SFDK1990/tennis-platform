package com.tennisplatform.administration.configuration;

import com.tennisplatform.administration.application.port.in.AdministerUsers;
import com.tennisplatform.administration.application.service.AdministerUsersService;
import com.tennisplatform.identity.application.port.in.AdministerAccounts;
import com.tennisplatform.student.application.port.in.ManageStudent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdministrationConfiguration {

    @Bean
    public AdministerUsers administerUsers(AdministerAccounts accounts, ManageStudent students) {
        return new AdministerUsersService(accounts, students);
    }
}
