package org.sonatype.nexus.jsecurity.realms.external.crowd;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.jsecurity.authc.AuthenticationException;
import org.jsecurity.authc.AuthenticationInfo;
import org.jsecurity.authc.DisabledAccountException;
import org.jsecurity.authc.IncorrectCredentialsException;
import org.jsecurity.authc.SimpleAuthenticationInfo;
import org.jsecurity.authc.UsernamePasswordToken;
import org.jsecurity.authz.AuthorizationException;
import org.jsecurity.authz.MissingAccountException;
import org.sonatype.nexus.jsecurity.realms.external.ExternalUserDataService;

import com.atlassian.crowd.integration.exception.ApplicationAccessDeniedException;
import com.atlassian.crowd.integration.exception.InactiveAccountException;
import com.atlassian.crowd.integration.exception.InvalidAuthenticationException;
import com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException;
import com.atlassian.crowd.integration.exception.ObjectNotFoundException;
import com.atlassian.crowd.integration.service.soap.client.ClientProperties;
import com.atlassian.crowd.integration.service.soap.client.ClientPropertiesImpl;
import com.atlassian.crowd.integration.service.soap.client.SecurityServerClient;
import com.atlassian.crowd.integration.service.soap.client.SecurityServerClientImpl;

public class CrowdUserDataService extends AbstractLogEnabled implements ExternalUserDataService,
        Initializable {

    private SecurityServerClient crowdClient;

    /**
     * @plexus.configuration
     */
    private Properties crowdProperties;

    /**
     * @plexus.configuration
     */
    private String rolePrefix;

    public List<String> getRoles(String username) {
        getLogger().info("Looking up role list for username: " + username);

        List<String> roles;
        try {
            roles = Arrays.asList(crowdClient.findRoleMemberships(username));
        } catch (RemoteException e) {
            throw new AuthorizationException("Unable to connect to Crowd.", e);
        } catch (InvalidAuthorizationTokenException e) {
            throw new AuthorizationException("Unable to connect to Crowd.", e);
        } catch (ObjectNotFoundException e) {
            throw new MissingAccountException("User '" + username + "' cannot be retrieved.", e);
        }

        if (rolePrefix != null) {
            for (int i = 0; i < roles.size(); i++) {
                String role = roles.get(i);
                if (role.startsWith(rolePrefix)) {
                    role = role.substring(rolePrefix.length());
                    roles.set(i, role);
                }
            }
        }

        getLogger().info("Obtained role list: " + roles.toString());

        return roles;
    }

    public AuthenticationInfo authenticate(UsernamePasswordToken token, String name) {
        try {
            crowdClient.authenticatePrincipalSimple(token.getUsername(), new String(token
                    .getPassword()));
            return new SimpleAuthenticationInfo(token.getPrincipal(), token.getCredentials(), name);
        } catch (RemoteException e) {
            throw new AuthenticationException("Could not retrieve info from Crowd.", e);
        } catch (InvalidAuthorizationTokenException e) {
            throw new AuthenticationException("Could not retrieve info from Crowd.", e);
        } catch (ApplicationAccessDeniedException e) {
            throw new AuthenticationException("Could not retrieve info from Crowd.", e);
        } catch (InvalidAuthenticationException e) {
            throw new IncorrectCredentialsException(e);
        } catch (InactiveAccountException e) {
            throw new DisabledAccountException(e);
        }
    }

    public void initialize() throws InitializationException {
        ClientProperties clientProps = new ClientPropertiesImpl(crowdProperties);
        crowdClient = new SecurityServerClientImpl(clientProps);
    }

}