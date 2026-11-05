package com.example.demo;
import org.springframework.test.context.ActiveProfiles;
@ActiveProfiles("spr-w6-d2-eager")
class Week6Day2EagerTests extends AbstractWeek6Day2RelationsTests {
    @Override boolean eager() {return true;}
}
