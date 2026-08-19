package org.fen.fen.controller;

import org.fen.fen.infra.WithSynteticId;
import org.fen.fen.service.BaseCrudService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract class BaseCrudControllerTestSupport<T extends WithSynteticId> {

    private MockMvc mockMvc;

    abstract String path();

    abstract BaseCrudController<T> controller();

    abstract BaseCrudService<T> service();

    abstract T entity();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void findAllReturnsPage() throws Exception {
        when(service().findAll(any(PageRequest.class))).thenReturn(Page.empty(PageRequest.of(0, 20)));

        mockMvc.perform(get(path()))
                .andExpect(status().isOk());

        verify(service()).findAll(any(PageRequest.class));
    }

    @Test
    void saveReturnsCreatedEntity() throws Exception {
        T entity = entity();
        when(service().save(any())).thenReturn(entity);

        mockMvc.perform(post(path())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());

        verify(service()).save(any());
    }

    @Test
    void updateReturnsUpdatedEntity() throws Exception {
        UUID id = UUID.randomUUID();
        T entity = entity();
        when(service().update(eq(id), any())).thenReturn(entity);

        mockMvc.perform(put(path() + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        verify(service()).update(eq(id), any());
    }

    @Test
    void deleteByIdReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete(path() + "/" + id))
                .andExpect(status().isNoContent());

        verify(service()).deleteById(id);
    }
}
