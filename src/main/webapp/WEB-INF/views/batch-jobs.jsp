<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <title>Batch Jobs - Document AI</title>
    <link rel="stylesheet" href="/webjars/bootstrap/5.3.0/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        .job-card {
            transition: all 0.3s;
            border-left: 4px solid transparent;
        }
        
        .job-card:hover {
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            transform: translateY(-2px);
        }
        
        .job-card.pending { border-left-color: #6c757d; }
        .job-card.running { border-left-color: #ffc107; }
        .job-card.completed { border-left-color: #28a745; }
        .job-card.failed { border-left-color: #dc3545; }
        .job-card.cancelled { border-left-color: #6c757d; }
        
        .progress-ring {
            width: 80px;
            height: 80px;
        }
        
        .stat-card {
            border-radius: 10px;
            padding: 20px;
            text-align: center;
            color: white;
        }
        
        .stat-card.total { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
        .stat-card.running { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
        .stat-card.completed { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
        .stat-card.failed { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); }
    </style>
</head>
<body>
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary">
        <div class="container">
            <a class="navbar-brand" href="/">
                <strong>📄 Document AI Analyzer</strong>
            </a>
            <div class="navbar-nav">
                <a class="nav-link" href="/">Upload</a>
                <a class="nav-link" href="/documents">Documents</a>
                <a class="nav-link" href="/batch">Batch Upload</a>
                <a class="nav-link active" href="/batch/jobs">Batch Jobs</a>
            </div>
        </div>
    </nav>

    <div class="container mt-4">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h1>
                <i class="fas fa-tasks"></i>
                Batch Jobs
            </h1>
            <div>
                <button class="btn btn-outline-primary" onclick="location.reload()">
                    <i class="fas fa-sync"></i> Refresh
                </button>
                <a href="/batch" class="btn btn-primary">
                    <i class="fas fa-plus"></i> New Batch Job
                </a>
            </div>
        </div>

        <!-- Statistics Cards -->
        <div class="row mb-4">
            <div class="col-md-3">
                <div class="stat-card total">
                    <h2 id="totalJobs">0</h2>
                    <p class="mb-0">Total Jobs</p>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card running">
                    <h2 id="runningJobs">0</h2>
                    <p class="mb-0">Running</p>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card completed">
                    <h2 id="completedJobs">0</h2>
                    <p class="mb-0">Completed</p>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card failed">
                    <h2 id="failedJobs">0</h2>
                    <p class="mb-0">Failed</p>
                </div>
            </div>
        </div>

        <!-- Batch Jobs List -->
        <c:choose>
            <c:when test="${empty batchJobs}">
                <div class="alert alert-info text-center">
                    <i class="fas fa-info-circle fa-3x mb-3"></i>
                    <h4>No Batch Jobs Yet</h4>
                    <p>Create your first batch job to process multiple documents at once.</p>
                    <a href="/batch" class="btn btn-primary">
                        <i class="fas fa-plus"></i> Create Batch Job
                    </a>
                </div>
            </c:when>
            <c:otherwise>
                <div class="row">
                    <c:forEach items="${batchJobs}" var="job">
                        <div class="col-md-6 mb-4">
                            <div class="card job-card ${job.status.toString().toLowerCase()}">
                                <div class="card-body">
                                    <div class="d-flex justify-content-between align-items-start mb-3">
                                        <div>
                                            <h5 class="card-title mb-1">
                                                <i class="fas fa-folder-open"></i>
                                                ${job.jobName}
                                            </h5>
                                            <small class="text-muted">
                                                Created: ${job.createdAt.toString().substring(0, 19).replace('T', ' ')}
                                            </small>
                                        </div>
                                        <c:choose>
                                            <c:when test="${job.status == 'PENDING'}">
                                                <span class="badge bg-secondary">Pending</span>
                                            </c:when>
                                            <c:when test="${job.status == 'RUNNING'}">
                                                <span class="badge bg-warning">
                                                    <i class="fas fa-spinner fa-spin"></i> Running
                                                </span>
                                            </c:when>
                                            <c:when test="${job.status == 'COMPLETED'}">
                                                <span class="badge bg-success">
                                                    <i class="fas fa-check"></i> Completed
                                                </span>
                                            </c:when>
                                            <c:when test="${job.status == 'FAILED'}">
                                                <span class="badge bg-danger">
                                                    <i class="fas fa-times"></i> Failed
                                                </span>
                                            </c:when>
                                            <c:when test="${job.status == 'CANCELLED'}">
                                                <span class="badge bg-secondary">Cancelled</span>
                                            </c:when>
                                        </c:choose>
                                    </div>

                                    <c:if test="${not empty job.description}">
                                        <p class="text-muted small">${job.description}</p>
                                    </c:if>

                                    <!-- Progress Bar -->
                                    <div class="mb-3">
                                        <div class="d-flex justify-content-between mb-1">
                                            <small>Progress</small>
                                            <small>
                                                <fmt:formatNumber value="${job.processedCount * 100.0 / job.totalDocuments}" 
                                                                  maxFractionDigits="1"/>%
                                            </small>
                                        </div>
                                        <div class="progress" style="height: 20px;">
                                            <div class="progress-bar ${job.status == 'COMPLETED' ? 'bg-success' : job.status == 'FAILED' ? 'bg-danger' : ''}" 
                                                 style="width: ${job.processedCount * 100.0 / job.totalDocuments}%">
                                                ${job.processedCount} / ${job.totalDocuments}
                                            </div>
                                        </div>
                                    </div>

                                    <!-- Statistics -->
                                    <div class="row g-2 mb-3">
                                        <div class="col-4">
                                            <div class="text-center p-2 bg-light rounded">
                                                <div class="h4 mb-0 text-primary">${job.totalDocuments}</div>
                                                <small class="text-muted">Total</small>
                                            </div>
                                        </div>
                                        <div class="col-4">
                                            <div class="text-center p-2 bg-light rounded">
                                                <div class="h4 mb-0 text-success">${job.successCount}</div>
                                                <small class="text-muted">Success</small>
                                            </div>
                                        </div>
                                        <div class="col-4">
                                            <div class="text-center p-2 bg-light rounded">
                                                <div class="h4 mb-0 text-danger">${job.failureCount}</div>
                                                <small class="text-muted">Failed</small>
                                            </div>
                                        </div>
                                    </div>

                                    <!-- Actions -->
                                    <div class="d-grid gap-2">
                                        <a href="/batch/${job.id}" class="btn btn-primary btn-sm">
                                            <i class="fas fa-eye"></i> View Details
                                        </a>
                                        <c:if test="${job.status == 'COMPLETED'}">
                                            <button class="btn btn-success btn-sm" onclick="exportResults('${job.id}')">
                                                <i class="fas fa-download"></i> Export Results
                                            </button>
                                        </c:if>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <script src="/webjars/jquery/3.7.1/jquery.min.js"></script>
    <script src="/webjars/bootstrap/5.3.0/js/bootstrap.bundle.min.js"></script>
    <script>
        // Calculate statistics
        function updateStatistics() {
            let total = 0, running = 0, completed = 0, failed = 0;
            
            <c:forEach items="${batchJobs}" var="job">
                total++;
                <c:choose>
                    <c:when test="${job.status == 'RUNNING'}">running++;</c:when>
                    <c:when test="${job.status == 'COMPLETED'}">completed++;</c:when>
                    <c:when test="${job.status == 'FAILED'}">failed++;</c:when>
                </c:choose>
            </c:forEach>
            
            document.getElementById('totalJobs').textContent = total;
            document.getElementById('runningJobs').textContent = running;
            document.getElementById('completedJobs').textContent = completed;
            document.getElementById('failedJobs').textContent = failed;
        }
        
        function exportResults(jobId) {
            window.location.href = '/api/batch/' + jobId + '/export/excel';
        }
        
        // Auto-refresh if any job is running
        let hasRunning = false;
        <c:forEach items="${batchJobs}" var="job">
            <c:if test="${job.status == 'RUNNING'}">
                hasRunning = true;
            </c:if>
        </c:forEach>
        
        if (hasRunning) {
            setTimeout(() => location.reload(), 5000); // Refresh every 5 seconds
        }
        
        updateStatistics();
    </script>
</body>
</html>