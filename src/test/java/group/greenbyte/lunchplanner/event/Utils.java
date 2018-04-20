package group.greenbyte.lunchplanner.event;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Date;

import static group.greenbyte.lunchplanner.Utils.createString;
import static group.greenbyte.lunchplanner.Utils.getJsonFromObject;

public class Utils {

    /**
     * Create an event
     * @return the id of the event
     */
    public static int createEvent(MockMvc mockMvc) throws Exception {
        long timeStart = System.currentTimeMillis() + 10000;
        long timeEnd = timeStart + 10000;
        int locationId = 1;

        EventJson event = new EventJson(createString(50), "", locationId, new Date(timeStart), new Date(timeEnd));

        String json = getJsonFromObject(event);

        MvcResult result = mockMvc.perform(
                MockMvcRequestBuilders.post("/event").contentType(MediaType.APPLICATION_JSON_VALUE).content(json))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andReturn();

        String response = result.getResponse().getContentAsString();

        return Integer.valueOf(response);
    }

    public static int createEvent(EventLogic eventLogic, String userName, int locationId) throws Exception {
        long timeStart = System.currentTimeMillis() + 10000;
        long timeEnd = timeStart + 10000;

        return eventLogic.createEvent(userName,
                createString(10),
                createString(10),
                locationId,
                new Date(timeStart),
                new Date(timeEnd));
    }

}