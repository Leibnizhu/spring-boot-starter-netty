package io.gitlab.leibnizhu.sbnetty.functional;

import io.gitlab.leibnizhu.sbnetty.bootstrap.EmbeddedNettyAutoConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Created by leibniz on 2017-08-24.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = EmbeddedNettyAutoConfiguration.class)
public class TestWebAppTester {
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Before
    public void setupMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    public void jsonTest() throws Exception {
        MvcResult result = mockMvc.perform(get("http://localhost:8080/json"))
//                .andExpect(MockMvcResultMatchers.content().json("{\"message\":\"Hello, World!\"}"))
                .andReturn();
        String resultStr = result.getResponse().getContentAsString();
        System.out.println(resultStr);
    }

}
