package com.github.apache9.auth;

import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;

import javax.security.auth.Subject;

/**
 * @author zhangduo
 */
public class ModifySubject {

    public static class NamePrincipal implements Principal {

        private final String name;

        public NamePrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

    }

    public static void main(String[] args) {
        char[] password = new char[] {
            'a', 'b', 'c'
        };
        Subject subject = new Subject();
        subject.getPrivateCredentials().add(password);
        subject.getPrincipals().add(new NamePrincipal("zhangduo"));
        Subject.doAs(subject, new PrivilegedAction<Object>() {

            @Override
            public Object run() {
                Subject subject = Subject.getSubject(AccessController
                        .getContext());
                String name = subject.getPrincipals(NamePrincipal.class)
                        .iterator().next().getName();
                char[] password = subject.getPrivateCredentials(char[].class)
                        .iterator().next();
                System.out.println(name + ": " + String.valueOf(password));
                return null;
            }

        });
    }
}
