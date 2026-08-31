# Event Storming: ServiceOrder

Actors: Customer and Administrative User.

Commands: CreateServiceOrder, StartDiagnosis, RequestApproval, ApproveQuotation, RejectQuotation, StartExecution, AddAdditionalRepairs, FinishServiceOrder, DeliverVehicle, TrackServiceOrder.

Domain events: ServiceOrderCreated, DiagnosisStarted, QuotationApprovalRequested, QuotationApproved, QuotationRejected, ExecutionStarted, AdditionalRepairsAdded, ServiceOrderFinished, VehicleDelivered.

Aggregates: ServiceOrder handles lifecycle, quotations, approvals, and snapshots. InventoryItem handles stock consumption triggered by execution.

Policies and rules:

- CreateServiceOrder requires an active Customer, an owned active Vehicle, and at least one active service.
- RequestApproval creates an immutable quotation version and moves the order to AWAITING_APPROVAL.
- ApproveQuotation/RejectQuotation require a valid customer access token and the current quotation version.
- StartExecution requires the current quotation to be approved and consumes outstanding stock atomically.
- AddAdditionalRepairs supersedes the previous quotation and requires customer approval again.

Read models: administrative details, customer tracking, service-order list by status, and execution-time history.

External systems: none in Phase 1.
