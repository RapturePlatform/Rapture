 <if(equalsTest)>
    assertTrue(payload.get<name; format="upcase">() == ret.get<name; format="upcase">());
 <else>
    assertTrue(payload.get<name; format="upcase">().equals(ret.get<name; format="upcase">()));
 <endif>
