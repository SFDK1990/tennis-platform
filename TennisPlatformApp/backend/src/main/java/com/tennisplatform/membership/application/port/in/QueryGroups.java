package com.tennisplatform.membership.application.port.in;

import java.util.List;

/** The groups of the school as any signed-in user reads them, without who is in them. */
public interface QueryGroups {

    List<GroupView> all();
}
