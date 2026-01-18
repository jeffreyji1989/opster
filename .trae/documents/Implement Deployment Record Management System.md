## Implementation Plan for Deployment Record Management

### 1. Create Deployment Record Module

#### Entity Layer (`DeploymentRecord.java`)
- Create new entity extending `BaseEntity`
- Fields: projectId, projectName, serverId, serverIp, serverAlias, serviceId, serviceName, status (IN_PROGRESS, COMPLETED, FAILED), logs
- Use appropriate JPA annotations and enums

#### Repository Layer (`DeploymentRecordRepository.java`)
- Extend `JpaRepository<DeploymentRecord, Integer>`
- Add custom query methods for filtering by project name (fuzzy search) and status
- Add method to find by creation time in descending order

#### Service Layer (`DeploymentRecordService.java`)
- Implement CRUD operations
- Implement query methods with filters
- Handle log storage and retrieval

#### Controller Layer (`DeploymentRecordController.java`)
- REST endpoints for:
  - GET /api/deployment-records - Query with filters (projectName, status)
  - GET /api/deployment-records/{id}/logs - View deployment logs

### 2. Modify Deployment Execution Logic

#### Update `ExecWebSocketHandler.java`
- Inject `DeploymentRecordRepository`
- Create deployment record at start of deployment with status IN_PROGRESS
- Append logs to the record as they are generated
- Update status to COMPLETED or FAILED based on execution result
- Save the record with final status and complete logs

### 3. Database Migration
- The entity creation will automatically generate the deployment_record table through JPA

### 4. Key Features
- **Record Generation**: Automatically create records when deployment is initiated
- **Status Tracking**: Track deployment status throughout the process
- **Log Storage**: Store complete deployment logs for review
- **Advanced Querying**: Support fuzzy search by project name and status filtering
- **Sorting**: Default sorting by creation time in descending order
- **Log Viewing**: Dedicated endpoint to view deployment logs

### 5. Technical Considerations
- Use existing codebase patterns and conventions
- Leverage `BaseEntity` for audit fields
- Ensure proper error handling and transaction management
- Maintain compatibility with existing deployment workflow
- Follow RESTful API design principles