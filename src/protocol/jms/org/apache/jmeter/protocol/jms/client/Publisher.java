/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.jmeter.protocol.jms.client;

import java.io.Serializable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;

import org.apache.jmeter.protocol.jms.Utils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

public class Publisher {

    private static final Logger log = LoggingManager.getLoggerForClass();

    private final TopicConnection connection;

    private final TopicSession session;

    private final Topic topic;

    private final TopicPublisher publisher;

    /**
     * Create a publisher using either the jndi.properties file or the provided parameters
     * @param useProps true if a jndi.properties file is to be used
     * @param initialContextFactory the (ignored if useProps is true)
     * @param providerUrl (ignored if useProps is true)
     * @param connfactory
     * @param topicName
     * @param useAuth (ignored if useProps is true)
     * @param securityPrincipal (ignored if useProps is true)
     * @param securityCredentials (ignored if useProps is true)
     * @throws JMSException if the context could not be initialised, or there was some other error
     * @throws NamingException 
     */
    public Publisher(boolean useProps, String initialContextFactory, String providerUrl, 
            String connfactory, String topicName, boolean useAuth,
            String securityPrincipal, String securityCredentials) throws JMSException, NamingException {
        super();
        Context ctx = initJNDI(useProps, initialContextFactory, 
                providerUrl, useAuth, securityPrincipal, securityCredentials);
        if (ctx == null){
            throw new JMSException("Could not initialize JNDI Initial Context Factory");
        }
        ConnectionFactory.getTopicConnectionFactory(ctx,connfactory);
        connection = ConnectionFactory.getTopicConnection();
        topic = Utils.lookupTopic(ctx, topicName);
        session = connection.createTopicSession(false, TopicSession.AUTO_ACKNOWLEDGE);
        publisher = session.createPublisher(topic);
        log.info("created the topic connection successfully");
    }

    private Context initJNDI(boolean useProps, String initialContextFactory, 
            String providerUrl, boolean useAuth, String securityPrincipal, String securityCredentials) {
        if (useProps) {
            try {
                return new InitialContext();
            } catch (NamingException e) {
                log.error(e.getMessage());
                return null;
            }
        } else {
            return InitialContextFactory.lookupContext(initialContextFactory, 
                    providerUrl, useAuth, securityPrincipal, securityCredentials);
        }
    }

    public void publish(String text) {
        try {
            TextMessage msg = session.createTextMessage(text);
            publisher.publish(msg);
        } catch (JMSException e) {
            log.error(e.getMessage());
        }
    }

    public void publish(Serializable contents) {
        try {
            ObjectMessage msg = session.createObjectMessage(contents);
            publisher.publish(msg);
        } catch (JMSException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Close will close the session
     */
    public void close() {
        Utils.close(publisher, log);
        Utils.close(session, log);
        Utils.close(connection, log);
    }
}
