package com.marcosperboni.notification.web;

import tools.jackson.databind.ObjectMapper;
import com.marcosperboni.notification.application.InvalidNotificationStateException;
import com.marcosperboni.notification.application.NotificationNotFoundException;
import com.marcosperboni.notification.application.NotificationService;
import com.marcosperboni.notification.domain.Notification;
import com.marcosperboni.notification.domain.NotificationChannel;
import com.marcosperboni.notification.domain.NotificationStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private NotificationService service;

	@Test
	void createReturns201ForValidRequest() throws Exception {
		UUID orderId = UUID.randomUUID();
		UUID customerId = UUID.randomUUID();
		Notification saved = Notification.create(orderId, customerId, NotificationChannel.EMAIL, "hello",
				NotificationStatus.SENT);
		when(service.send(eq(orderId), eq(customerId), eq(NotificationChannel.EMAIL), eq("hello")))
				.thenReturn(saved);

		mockMvc.perform(post("/api/notifications")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new NotificationRequest(orderId, customerId, NotificationChannel.EMAIL, "hello"))))
				.andExpect(status().isCreated());
	}

	@Test
	void createReturns400ForBlankMessage() throws Exception {
		mockMvc.perform(post("/api/notifications")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new NotificationRequest(UUID.randomUUID(), UUID.randomUUID(), NotificationChannel.EMAIL, " "))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createReturns400ForMissingRequiredFields() throws Exception {
		mockMvc.perform(post("/api/notifications")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void getReturns404WhenMissing() throws Exception {
		UUID id = UUID.randomUUID();
		when(service.getById(id)).thenThrow(new NotificationNotFoundException(id));

		mockMvc.perform(get("/api/notifications/{id}", id)).andExpect(status().isNotFound());
	}

	@Test
	void getReturns200WhenFound() throws Exception {
		UUID id = UUID.randomUUID();
		Notification notification = Notification.create(UUID.randomUUID(), UUID.randomUUID(),
				NotificationChannel.EMAIL, "hi", NotificationStatus.SENT);
		when(service.getById(id)).thenReturn(notification);

		mockMvc.perform(get("/api/notifications/{id}", id)).andExpect(status().isOk());
	}

	@Test
	void listReturns200() throws Exception {
		when(service.list(isNull(), isNull(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

		mockMvc.perform(get("/api/notifications")).andExpect(status().isOk());
	}

	@Test
	void putReturns200OnValidTransition() throws Exception {
		UUID id = UUID.randomUUID();
		Notification updated = Notification.create(UUID.randomUUID(), UUID.randomUUID(), NotificationChannel.EMAIL,
				"updated", NotificationStatus.SENT);
		when(service.update(eq(id), eq(NotificationStatus.SENT), eq("updated"))).thenReturn(updated);

		mockMvc.perform(put("/api/notifications/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new NotificationUpdateRequest(NotificationStatus.SENT, "updated"))))
				.andExpect(status().isOk());
	}

	@Test
	void putReturns404WhenMissing() throws Exception {
		UUID id = UUID.randomUUID();
		when(service.update(eq(id), any(), any())).thenThrow(new NotificationNotFoundException(id));

		mockMvc.perform(put("/api/notifications/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new NotificationUpdateRequest(null, "x"))))
				.andExpect(status().isNotFound());
	}

	@Test
	void putReturns409OnIllegalTransition() throws Exception {
		UUID id = UUID.randomUUID();
		when(service.update(eq(id), eq(NotificationStatus.FAILED), isNull()))
				.thenThrow(new InvalidNotificationStateException("illegal"));

		mockMvc.perform(put("/api/notifications/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new NotificationUpdateRequest(NotificationStatus.FAILED, null))))
				.andExpect(status().isConflict());
	}

	@Test
	void deleteReturns204() throws Exception {
		UUID id = UUID.randomUUID();

		mockMvc.perform(delete("/api/notifications/{id}", id)).andExpect(status().isNoContent());
	}

	@Test
	void deleteReturns404WhenMissing() throws Exception {
		UUID id = UUID.randomUUID();
		org.mockito.Mockito.doThrow(new NotificationNotFoundException(id)).when(service).delete(id);

		mockMvc.perform(delete("/api/notifications/{id}", id)).andExpect(status().isNotFound());
	}
}
