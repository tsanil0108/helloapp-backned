package com.packersmovers.marketplace.service;

import com.packersmovers.marketplace.dto.common.PageResponse;
import com.packersmovers.marketplace.dto.lead.CustomerLeadRequest;
import com.packersmovers.marketplace.dto.lead.LeadResponse;
import com.packersmovers.marketplace.dto.lead.LeadStatusUpdateRequest;

import java.util.List;

public interface LeadService {

    /** Public, no-login entry point: create a lead from the customer form, run quality checks, then hand off to matching. */
    LeadResponse createLead(CustomerLeadRequest request, String ipAddress);

    LeadResponse getByCode(String leadCode);

    PageResponse<LeadResponse> search(String status, int page, int size);

    List<LeadResponse> myRequests(String mobile);

    LeadResponse updateStatus(Long leadId, LeadStatusUpdateRequest request);
}
