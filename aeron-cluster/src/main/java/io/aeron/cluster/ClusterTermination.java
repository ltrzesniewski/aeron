/*
 * Copyright 2014-2020 Real Logic Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.aeron.cluster;

class ClusterTermination
{
    private long deadlineNs;
    private boolean haveServicesTerminated = false;

    ClusterTermination(final long deadlineNs)
    {
        this.deadlineNs = deadlineNs;
    }

    void deadlineNs(final long deadlineNs)
    {
        this.deadlineNs = deadlineNs;
    }

    boolean canTerminate(final ClusterMember[] members, final long terminationPosition, final long nowNs)
    {
        if (haveServicesTerminated)
        {
            return haveFollowersTerminated(members, terminationPosition) || nowNs >= deadlineNs;
        }

        return false;
    }

    void onServicesTerminated()
    {
        haveServicesTerminated = true;
    }

    void terminationPosition(
        final MemberStatusPublisher memberStatusPublisher,
        final ClusterMember[] members,
        final ClusterMember thisMember,
        final long position)
    {
        for (final ClusterMember member : members)
        {
            member.hasTerminated(false);

            if (member != thisMember)
            {
                memberStatusPublisher.terminationPosition(member.publication(), position);
            }
        }
    }

    private static boolean haveFollowersTerminated(final ClusterMember[] members, final long terminationPosition)
    {
        boolean result = true;

        for (int i = 0, length = members.length; i < length; i++)
        {
            final ClusterMember member = members[i];

            if (!member.hasTerminated() && member.logPosition() < terminationPosition)
            {
                result = false;
                break;
            }
        }

        return result;
    }
}
