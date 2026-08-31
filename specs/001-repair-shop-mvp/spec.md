# Feature Specification: Automotive Repair Shop MVP

**Feature Branch**: `001-repair-shop-mvp`

**Created**: 2026-08-31

**Status**: Draft

**Input**: User description: "Build the MVP backend for an integrated automotive repair shop service
management system covering customers, vehicles, service catalog, inventory, service orders, quotations,
approvals, workflow tracking, authentication, and execution-time monitoring."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Manage Repair Shop Records (Priority: P1)

An authenticated administrative user registers and maintains customers, their vehicles, the services
offered by the shop, and stocked parts and supplies. These records form a reliable source for service
orders and quotations, replacing duplicate notes and spreadsheets.

**Why this priority**: The shop cannot create a valid service order or quotation without trustworthy
customer, vehicle, service, and inventory records.

**Independent Test**: Register a customer with a valid tax identifier, attach a vehicle, create a priced
service and stocked part, retrieve and update each record, and verify invalid identifiers, duplicate
identifiers, and impossible stock values are rejected.

**Acceptance Scenarios**:

1. **Given** an authenticated administrator and a valid, unused CPF or CNPJ, **When** the administrator
   registers a customer, **Then** the customer can be found by that identifier and its details are stored.
2. **Given** a registered customer, **When** the administrator registers a vehicle with a valid license
   plate, brand, model, and year, **Then** the vehicle appears in that customer's vehicle list.
3. **Given** an existing service and inventory item, **When** the administrator changes their current
   prices or available stock, **Then** new operations use the updated values without changing historical
   service-order values.
4. **Given** an invalid CPF, CNPJ, license plate, year, price, or stock quantity, **When** registration or
   update is attempted, **Then** the operation is rejected with actionable validation details.
5. **Given** a customer, vehicle, service, or inventory item referenced by a service order, **When** its
   removal is requested, **Then** historical references remain intact and the record becomes unavailable
   for new work rather than being destroyed.

---

### User Story 2 - Create and Approve a Quotation (Priority: P1)

An authenticated administrator creates a service order for a known customer and that customer's vehicle,
selects requested services and required parts or supplies, and presents the automatically calculated
quotation for customer approval.

**Why this priority**: A traceable service order and approved quotation are the central replacement for
the repair shop's manual workflow.

**Independent Test**: Create an order from registered records, verify its line-item and total values,
submit it for approval, record approval through the customer approval flow, and confirm it becomes
eligible for execution while all quoted values remain unchanged.

**Acceptance Scenarios**:

1. **Given** a customer with a registered vehicle and active catalog items, **When** an administrator
   creates a service order with at least one requested service, **Then** the order starts as Received and
   references the correct customer and vehicle.
2. **Given** selected services, parts, supplies, quantities, and current prices, **When** the quotation is
   calculated, **Then** each line value and the total equal the sum of the selected quantities and prices.
3. **Given** a calculated quotation, **When** it is submitted to the customer, **Then** the order enters
   Awaiting Approval and exposes the quoted scope and total through the approval flow.
4. **Given** a pending quotation, **When** the customer approves it using the order's customer-access
   credential, **Then** the approval, quotation version, and approval time are recorded and the approved
   order can enter In Execution.
5. **Given** a vehicle that does not belong to the identified customer or an inactive catalog item,
   **When** order creation is attempted, **Then** creation is rejected without a partial service order.

---

### User Story 3 - Execute a Controlled Service Order (Priority: P1)

An authenticated administrator advances a service order through diagnosis, approval, execution,
completion, and delivery while the system enforces the workflow, approvals, and inventory consistency.

**Why this priority**: Controlled execution prevents unauthorized repairs, lost history, and stock errors.

**Independent Test**: Move an approved order through its valid lifecycle, consume the required stock,
add a newly discovered repair requiring another approval, resume execution after approval, finish and
deliver the order, and verify every transition and historical event.

**Acceptance Scenarios**:

1. **Given** a service order in any lifecycle state, **When** an administrator requests an allowed next
   state, **Then** the state changes once and a timestamped history entry identifies the transition.
2. **Given** a service order in any lifecycle state, **When** an invalid transition is requested, **Then**
   it is rejected and the order, approval, and inventory state remain unchanged.
3. **Given** a quotation awaiting required approval, **When** execution is requested, **Then** execution
   is rejected and no inventory is consumed.
4. **Given** an approved order with sufficient stock, **When** execution begins, **Then** the required
   quantities are consumed exactly once and stock cannot become negative.
5. **Given** an order already in execution and a newly required repair that changes the approved total,
   **When** the repair is added, **Then** a new quotation version is created, the order returns to
   Awaiting Approval, and additional work cannot proceed until that version is approved.
6. **Given** a Finished or Delivered service order, **When** referenced catalog prices or customer data
   later change, **Then** the order's historical customer, vehicle, line-item, approval, and total details
   remain as recorded.

---

### User Story 4 - Track Service Order Progress (Priority: P2)

A customer uses a restricted access flow to view the current progress of their service order, while an
authenticated administrator lists active orders and views complete operational details.

**Why this priority**: Timely, safe tracking reduces calls to the shop and gives both customers and staff
a shared view of progress.

**Independent Test**: Query an order with its valid customer-access credential and verify the limited
tracking view, then query as an administrator and verify the complete view and active-order listing.

**Acceptance Scenarios**:

1. **Given** a valid service-order tracking credential, **When** the customer requests tracking, **Then**
   the response identifies the order, current status, understandable progress, and last relevant update
   without administrative or sensitive information.
2. **Given** a missing, invalid, expired, or mismatched tracking credential, **When** tracking is requested,
   **Then** no service-order details are disclosed.
3. **Given** an authenticated administrator, **When** service orders are listed, **Then** the administrator
   can filter or page through orders and identify those currently being handled.
4. **Given** an authenticated administrator and an existing order, **When** complete details are requested,
   **Then** the response includes its customer and vehicle snapshots, quotation versions, approvals,
   inventory lines, status, and transition history.

---

### User Story 5 - Monitor Execution Time (Priority: P3)

An authenticated administrative user views the average time spent executing completed services, derived
from lifecycle events rather than manually entered aggregate figures.

**Why this priority**: The metric gives the shop an objective operational baseline after its core workflow
is reliable.

**Independent Test**: Complete multiple orders with known execution entry and completion timestamps,
request the metric for a date range, and verify the average uses eligible lifecycle intervals only.

**Acceptance Scenarios**:

1. **Given** Finished or Delivered orders that entered In Execution, **When** an administrator requests
   average execution time for a period, **Then** the result is calculated from each order's execution-start
   and finish lifecycle events and reports the number of included orders.
2. **Given** no eligible completed orders in the requested period, **When** the metric is requested,
   **Then** the response clearly reports that no average is available and uses no invented zero-duration
   observations.
3. **Given** an order that paused for additional approval after execution began, **When** its execution
   time is calculated, **Then** time spent Awaiting Approval is excluded from active execution time.

### Edge Cases

- A CPF or CNPJ already assigned to a customer, or a license plate already assigned to a vehicle, is
  rejected as a duplicate after normalization.
- Concurrent changes to the same service order or stock item cannot both succeed if together they would
  violate the current workflow, approval version, or available-stock invariant.
- A quotation rejects zero or negative quantities, negative prices, missing catalog records, and totals
  that cannot be represented accurately as currency.
- An approval for an obsolete quotation version does not authorize the current version.
- Repeating an approval, transition, or stock-consumption request does not duplicate the business effect.
- Removing or deactivating a record used by an open service order does not make that order unreadable and
  does not alter its recorded quotation.
- A customer tracking response remains limited even when the credential references a valid order; it
  never returns CPF, CNPJ, internal notes, stock levels, credentials, or unrelated customer information.
- Pagination or bounded result limits apply when customer vehicles, catalog items, inventory, service
  orders, or history grows beyond a single response.
- If execution starts and finishes more than once because of additional approval, active intervals are
  summed and approval waiting intervals are excluded from the execution-time metric.
- A vehicle year outside a plausible accepted range is rejected consistently during registration and
  update.

## Requirements *(mandatory)*

### Functional Requirements

#### Access and Security

- **FR-001**: The system MUST authenticate administrative users before allowing any administrative
  operation and MUST reject absent, invalid, or expired credentials.
- **FR-002**: The system MUST authorize administrative operations separately from customer tracking and
  approval operations.
- **FR-003**: The system MUST issue or associate an unguessable, order-specific customer-access credential
  for quotation approval and tracking, and MUST allow that credential to be revoked or expired.
- **FR-004**: Customer-access responses MUST expose only the order information required for approval or
  progress tracking and MUST exclude credentials, secrets, internal notes, sensitive identifiers,
  administrative data, and information belonging to other customers.
- **FR-005**: Validation and error responses MUST be consistent and actionable without exposing internal
  implementation details.
- **FR-006**: Important authentication failures, approval decisions, status transitions, inventory
  changes, and business-rule rejections MUST be auditable without recording secrets or full sensitive
  identifiers.

#### Customer and Vehicle Management

- **FR-007**: An authenticated administrator MUST be able to register, retrieve, update, list, and remove
  customers when removal is allowed.
- **FR-008**: The system MUST normalize and validate CPF and CNPJ values and MUST enforce uniqueness of
  the normalized identifier across customers.
- **FR-009**: An authenticated administrator MUST be able to find exactly one customer by CPF or CNPJ.
- **FR-010**: An authenticated administrator MUST be able to register, retrieve, update, list, and remove
  vehicles associated with a customer when removal is allowed.
- **FR-011**: Every vehicle MUST record a normalized unique license plate, brand, model, year, and owner.
- **FR-012**: The system MUST validate Brazilian license plate formats and plausible vehicle years.
- **FR-013**: A vehicle selected for a service order MUST belong to that order's customer.
- **FR-014**: A customer or vehicle referenced by a service order MUST retain its historical reference;
  removal MUST make it unavailable for new work rather than erase required history.

#### Service Catalog and Inventory

- **FR-015**: An authenticated administrator MUST be able to register, retrieve, update, list, and remove
  offered services, each with a name, description, current non-negative price, and active state.
- **FR-016**: An authenticated administrator MUST be able to register, retrieve, update, list, and remove
  parts and supplies, each with a name, current non-negative unit price, available stock quantity, and
  active state.
- **FR-017**: Inventory changes MUST identify the item, quantity, reason, time, and resulting balance.
- **FR-018**: The system MUST reject any reservation, consumption, return, or adjustment that would result
  in an invalid stock quantity.
- **FR-019**: Stock consumption for a service order MUST occur exactly once for each approved executable
  item and MUST remain associated with that order.
- **FR-020**: Removing a service or inventory item already referenced by an order MUST preserve its
  historical line-item details and MUST prevent selection for new quotations.

#### Service Orders, Quotations, and Approvals

- **FR-021**: An authenticated administrator MUST be able to create a service order for one valid customer
  and one vehicle owned by that customer, with at least one requested registered service.
- **FR-022**: A new service order MUST begin in Received status with a unique tracking identity and a
  timestamped creation event.
- **FR-023**: An order MAY contain service, part, and supply line items with positive quantities.
- **FR-024**: The system MUST calculate each quotation line from its captured unit price and quantity and
  calculate the total as the exact sum of all lines.
- **FR-025**: Each quotation version MUST preserve its services, parts, supplies, quantities, unit prices,
  line totals, overall total, creation time, and approval state regardless of later catalog changes.
- **FR-026**: The system MUST support submitting the current quotation version for customer approval and
  recording an approval or rejection with its quotation version and decision time.
- **FR-027**: Approval or rejection MUST apply only to the current pending quotation version and MUST NOT
  be reused after the quotation changes.
- **FR-028**: An authenticated administrator MUST be able to add additional repair items discovered during
  diagnosis or execution; if they change approved scope or price, the system MUST create a new quotation
  version and require new customer approval.
- **FR-029**: A service order MUST NOT enter or resume In Execution while its current quotation requires
  approval and lacks recorded approval.
- **FR-030**: Quotation, approval, status, and inventory changes that form one business action MUST either
  all succeed or leave the prior service-order state unchanged.

#### Controlled Workflow

- **FR-031**: The only supported service-order statuses MUST be Received, In Diagnosis, Awaiting Approval,
  In Execution, Finished, and Delivered.
- **FR-032**: The standard forward transitions MUST be Received to In Diagnosis, In Diagnosis to Awaiting
  Approval, Awaiting Approval to In Execution after approval, In Execution to Finished, and Finished to
  Delivered.
- **FR-033**: In Execution MAY transition to Awaiting Approval only when additional repair items require
  authorization; after approval it MAY return to In Execution.
- **FR-034**: Every other status transition MUST be rejected without changing the order or related stock.
- **FR-035**: Every successful transition MUST preserve the previous state and record the new state,
  occurrence time, responsible actor or customer action, and reason when applicable.
- **FR-036**: Finished and Delivered orders MUST be immutable with respect to quoted scope, approvals,
  consumed inventory, customer and vehicle snapshots, and lifecycle history.
- **FR-037**: An authenticated administrator MUST be able to list service orders with bounded pagination,
  filter by status, identify open orders, and retrieve complete details for one order.

#### Tracking and Metrics

- **FR-038**: A customer with a valid order-specific access credential MUST be able to retrieve the order
  identity, current status, customer-readable progress, and last relevant update.
- **FR-039**: Customer tracking MUST NOT require or grant administrative access.
- **FR-040**: The system MUST record execution entry, execution exit, finish, and delivery times from
  service-order lifecycle transitions.
- **FR-041**: An authenticated administrator MUST be able to request average active execution time for
  eligible Finished and Delivered orders over a specified time period.
- **FR-042**: Active execution time MUST be derived from lifecycle intervals in In Execution and MUST
  exclude intervals spent Awaiting Approval.
- **FR-043**: The execution-time result MUST report the requested period, average duration, and number of
  eligible service orders, and MUST distinguish an empty data set from a zero-duration average.

#### API and Operational Behavior

- **FR-044**: All capabilities MUST be available through consistent, documented service interfaces with
  predictable resource naming, operation semantics, validation behavior, status outcomes, pagination,
  and a shared error format.
- **FR-045**: The system MUST provide machine-readable documentation for administrative, tracking, and
  approval operations, their credentials, inputs, outcomes, and error cases.
- **FR-046**: List operations MUST use bounded responses and support navigation through additional results.
- **FR-047**: The system MUST preserve sufficient timestamps and audit history to reconstruct each service
  order's quotation approvals, lifecycle, inventory effects, and active execution duration.
- **FR-048**: The complete backend and its required local dependencies MUST be startable in a reproducible
  local environment using repository-provided instructions.
- **FR-049**: The repository instructions MUST allow a new developer to configure, start, verify, test,
  and stop the application without undocumented setup steps.

### Key Entities

- **Administrative User**: A repair-shop staff identity permitted to perform protected management and
  operational actions; carries authentication status and authorization role.
- **Customer**: A person or organization receiving repair services; identified uniquely by a normalized
  CPF or CNPJ and associated with contact details and vehicles.
- **Vehicle**: A customer-owned vehicle identified by license plate, brand, model, and year; may belong to
  only one current customer for the purposes of this MVP.
- **Catalog Service**: A repair or maintenance service offered by the shop, with descriptive information,
  a current price, and availability for new quotations.
- **Inventory Item**: A part or supply with a current unit price, available quantity, and activity state.
- **Inventory Movement**: An auditable increase, decrease, reservation, return, or adjustment linked to an
  item and, when applicable, a service order.
- **Service Order**: The aggregate that coordinates one customer, one vehicle, requested work, quotation
  versions, approvals, inventory effects, lifecycle state, tracking identity, and history.
- **Quotation**: An immutable versioned commercial proposal for an order, composed of service, part, and
  supply lines with captured quantities and prices.
- **Quotation Line**: A captured service, part, or supply description, quantity, unit price, and line total
  belonging to one quotation version.
- **Customer Approval**: A customer's approve or reject decision for exactly one quotation version,
  including decision state and time.
- **Status History Entry**: An immutable record of a service-order transition, actor, time, and reason.
- **Customer Access Credential**: A revocable, expirable, order-specific secret that permits only the
  customer's tracking and approval actions for that order.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An administrator can register a customer, vehicle, catalog service, and inventory item and
  create a complete service order from them in under 10 minutes without using external notes.
- **SC-002**: In acceptance testing, 100% of quotations equal the exact sum of their captured service,
  part, and supply lines, including after catalog prices change.
- **SC-003**: In acceptance testing, 100% of invalid service-order transitions and attempts to execute an
  unapproved quotation are rejected without changing order or inventory state.
- **SC-004**: In concurrent stock tests, 100% of completed operations preserve a non-negative available
  quantity and each approved service-order consumption is applied no more than once.
- **SC-005**: At least 95% of valid customer, vehicle, catalog, inventory, order-detail, and tracking
  requests complete within 2 seconds under a representative workload of 50 simultaneous users and
  100,000 total persisted business records.
- **SC-006**: A customer with valid access can identify the order's current status and progress in under
  30 seconds, while all unauthorized tracking attempts disclose no order details.
- **SC-007**: 100% of Finished and Delivered orders retain their recorded quotations, approvals,
  inventory effects, and status histories after related master records are updated or deactivated.
- **SC-008**: Average active execution time for any requested period matches calculations independently
  derived from lifecycle history for 100% of acceptance-test data sets.
- **SC-009**: Automated tests cover at least 80% of each critical business domain and all defined critical
  workflows pass repeatably across three consecutive clean test runs.
- **SC-010**: A new developer can start and verify the complete local backend within 15 minutes by using
  only the repository instructions and documented prerequisites.
- **SC-011**: All administrative operations reject unauthenticated access, and security review finds no
  customer response or log entry exposing credentials, tokens, full sensitive identifiers, password
  material, or internal error details.
- **SC-012**: During a four-week pilot, staff can trace 100% of active service orders to a customer,
  vehicle, current status, current quotation approval state, and inventory impact without consulting a
  spreadsheet or paper note.

## Assumptions

- The MVP serves one repair-shop location, one inventory pool, and one currency.
- CPF, CNPJ, and license plates follow Brazilian validation and normalization rules.
- Administrative identities are provisioned outside this feature; self-registration, password recovery,
  and staff-account administration are not MVP capabilities.
- All authenticated administrative users have the same operational permissions in the MVP; finer-grained
  staff roles may be added only through a later approved specification.
- Customer approval records the decision but does not implement electronic signatures, identity proofing,
  payments, or regulatory consent workflows.
- The order-specific customer-access credential is delivered to the customer through an out-of-band shop
  process; email, SMS, and messaging integrations are outside scope.
- Removal means deactivation when a record has historical references; unreferenced records may be removed
  according to business rules while audit requirements remain satisfied.
- Prices captured in a quotation include the final amounts used by the MVP; tax, discount, and promotion
  engines are outside scope unless explicitly added later.
- Required inventory is consumed when an approved order enters execution. Adding approved items later
  consumes only their outstanding quantities when execution resumes.
- Average execution time uses active In Execution intervals and excludes time awaiting customer approval.
- Scheduling, appointments, payments, frontend applications, mobile applications, microservices, event
  brokers, and cloud infrastructure are outside the MVP.
- The representative workload in SC-005 is the planning baseline for a medium-sized repair shop and may
  be refined only with documented operational evidence.
