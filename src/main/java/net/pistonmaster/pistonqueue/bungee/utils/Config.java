/*
 * #%L
 * PistonQueue
 * %%
 * Copyright (C) 2021 AlexProgrammerDE
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package net.pistonmaster.pistonqueue.bungee.utils;

import java.util.List;

/**
 * Config
 */
public final class Config {
    public static String SERVERISFULLMESSAGE, QUEUEPOSITION, JOININGMAINSERVER,
            QUEUEBYPASSPERMISSION, QUEUESERVER, QUEUEPRIORITYPERMISSION, REGEX,
            KICKMESSAGE, ADMINPERMISSION, MAINSERVER, AUTHSERVER, SERVERDOWNKICKMESSAGE,
            CUSTOMPROTOCOL, QUEUEVETERANPERMISSION, REGEXMESSAGE, PAUSEQUEUEIFMAINDOWNMESSAGE,
            SHADOWBANMESSAGE, IFMAINDOWNSENDTOQUEUEMESSAGE, RECOVERYMESSAGE;

    public static boolean POSITIONMESSAGEHOTBAR, ENABLEKICKMESSAGE, SERVERPINGINFOENABLE,
            ENABLEAUTHSERVER, ALWAYSQUEUE, CUSTOMPROTOCOLENABLE, REGISTERTAB, AUTHFIRST, POSITIONMESSAGECHAT,
            PAUSEQUEUEIFMAINDOWN, KICKWHENDOWN, FORCEMAINSERVER, ALLOWAUTHSKIP, IFMAINDOWNSENDTOQUEUE, RECOVERY;

    public static int MAINSERVERSLOTS, QUEUEMOVEDELAY, QUEUESERVERSLOTS, SERVERONLINECHECKDELAY, POSITIONMESSAGEDELAY, STARTTIME;

    public static List<String> SERVERPINGINFO, HEADER, FOOTER, HEADERPRIORITY, FOOTERPRIORITY, HEADERVETERAN, FOOTERVETERAN, DOWNWORDLIST;
}
