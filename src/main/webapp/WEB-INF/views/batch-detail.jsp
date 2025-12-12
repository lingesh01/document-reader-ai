<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <title>Batch Job Details - ${batchJob.jobName}</title>
    <link rel="stylesheet" href="/webjars/bootstrap/5.3.0/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        .status-indicator {
            width: 12px;
            height: 12px;
            border-radius: 50%;
            display: inline-block;
            margin-right: 5px;
        }
        
        .status-uploaded { background: #6c757d; }
        .status-processing { background: #ffc107; animation: pulse 2s infinite; }
        .status-ready { background: #17a2b8; }
        .status-analyzing { background: #fd7e14; animation: pulse 2s infinite; }
        .status-analyzed { background: #28a745; }
        .status-failed { background: #dc3545; }
        
        @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.5; }
        }
        
        .doc-item {
            border-left: 4px solid transparent;
            transition: all 0.2s;
        }
        
        .doc-item:hover {
            background: #f8f9fa;
        }
        
        .doc-item.status-analyzed { border-left-color: #28a745; }
        .doc-item.status-failed { border-left-color: #dc3545; }
        .doc-item.status-processing { border-left-color: #ffc107; }
        
        .time-badge {
            font-family: monospace;
            background: #e9ecef;
            padding: 2px 8px;
            border-radius: 3px;
        }
    </style>
</head>
<body>
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary">
        <div class="container-fluid">
            <a class="navbar-brand" href="/">
                <strong>📄 Document AI Analyzer</strong>
            </a>
            <div class="navbar-nav">
                <a class="nav-link" href="/batch/jobs">← Back to Jobs</a>
            </div>
        </div>
    </nav>

    <div class="container-fluid mt-4">
        <!-- Job Header -->
        <div class="card mb-4">
            <div class="card-header bg-primary text-white">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <h4 class="mb-0">
                            <i class="fas fa-briefcase"></i>
                            ${batchJob.jobName}
                        </h4>
                        <small>Job ID: ${batchJob.id}</small>
                    </div>
                    <div>
                        <c:choose>
                            <c:when test="${batchJob.status == 'RUNNING'}">
                                <span class="badge bg-warning fs-5">
                                    <i class="fas fa-spinner fa-spin"></i> Running
                                </span>
                            </c:when>
                            <c:when test="${batchJob.status == 'COMPLETED'}">
                                <span class="badge bg-success fs-5">
                                    <i class="fas fa-check-circle"></i> Completed
                                </span>
                            </c:when>
                            <c:when test="${batchJob.status == 'FAILED'}">
                                <span class="badge bg-danger fs-5">
                                    <i class="fas fa-exclamation-circle"></i> Failed
                                </span>
                            </c:when>
                            <c:otherwise>
                                <span class="badge bg-secondary fs-5">${batchJob.status}</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>
            <div class="card-body">
                <c:if test="${not empty batchJob.description}">
                    <p class="mb-3"><strong>Description:</strong> ${batchJob.description}</p>
                </c:if>
                
                <!-- Progress Bar -->
                <div class="mb-3">
                    <div class="d-flex justify-content-between mb-2">
                        <strong>Overall Progress</strong>
                        <span id="progressText">
                            <span id="processedCount">${batchJob.processedCount}</span> / 
                            <span id="totalCount">${batchJob.totalDocuments}</span>
                            (<span id="progressPercent">
                                <fmt:formatNumber value="${batchJob.processedCount * 100.0 / batchJob.totalDocuments}" 
                                                  maxFractionDigits="1"/>
                            </span>%)
                        </span>
                    </div>
                    <div class="progress" style="height: 30px;">
                        <div class="progress-bar progress-bar-striped ${batchJob.status == 'RUNNING' ? 'progress-bar-animated' : ''} 
                                    ${batchJob.status == 'COMPLETED' ? 'bg-success' : batchJob.status == 'FAILED' ? 'bg-danger' : ''}" 
                             id="mainProgressBar"
                             style="width: ${batchJob.processedCount * 100.0 / batchJob.totalDocuments}%">
                        </div>
                    </div>
                </div>

                <!-- Statistics Row -->
                <div class="row g-3">
                    <div class="col-md-3">
                        <div class="card bg-light">
                            <div class="card-body text-center">
                                <h3 class="text-primary" id="totalDocs">${batchJob.totalDocuments}</h3>
                                <p class="mb-0 text-muted">Total Documents</p>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="card bg-light">
                            <div class="card-body text-center">
                                <h3 class="text-success" id="successDocs">${batchJob.successCount}</h3>
                                <p class="mb-0 text-muted">Successful</p>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="card bg-light">
                            <div class="card-body text-center">
                                <h3 class="text-danger" id="failedDocs">${batchJob.failureCount}</h3>
                                <p class="mb-0 text-muted">Failed</p>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="card bg-light">
                            <div class="card-body text-center">
                                <h3 class="text-warning" id="remainingDocs">
                                    ${batchJob.totalDocuments - batchJob.processedCount}
                                </h3>
                                <p class="mb-0 text-muted">Remaining</p>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Time Info -->
                <div class="mt-3 d-flex justify-content-between">
                    <div>
                        <strong>Started:</strong> 
                        <span class="time-badge">
                            ${batchJob.startedAt != null ? batchJob.startedAt.toString().substring(0, 19).replace('T', ' ') : 'Not started'}
                        </span>
                    </div>
                    <div>
                        <strong>Completed:</strong> 
                        <span class="time-badge">
                            ${batchJob.completedAt != null ? batchJob.completedAt.toString().substring(0, 19).replace('T', ' ') : 'In progress'}
                        </span>
                    </div>
                    <div id="etaContainer" style="display: ${batchJob.status == 'RUNNING' ? 'block' : 'none'}">
                        <strong>ETA:</strong> 
                        <span class="time-badge" id="eta">Calculating...</span>
                    </div>
                </div>

                <!-- Action Buttons -->
                <div class="mt-3 d-flex gap-2">
                    <c:if test="${batchJob.status == 'COMPLETED'}">
                        <button class="btn btn-success" onclick="exportToExcel()">
                            <i class="fas fa-file-excel"></i> Export to Excel
                        </button>
                        <button class="btn btn-outline-success" onclick="exportToCSV()">
                            <i class="fas fa-file-csv"></i> Export to CSV
                        </button>
                    </c:if>
                    <c:if test="${batchJob.status == 'RUNNING'}">
                        <button class="btn btn-warning" onclick="pauseJob()">
                            <i class="fas fa-pause"></i> Pause
                        </button>
                        <button class="btn btn-danger" onclick="cancelJob()">
                            <i class="fas fa-stop"></i> Cancel
                        </button>
                    </c:if>
                    <button class="btn btn-outline-primary" onclick="location.reload()">
                        <i class="fas fa-sync"></i> Refresh
                    </button>
                </div>
            </div>
        </div>

        <!-- Documents Table -->
        <div class="card">
            <div class="card-header bg-secondary text-white">
                <h5 class="mb-0">
                    <i class="fas fa-list"></i>
                    Documents in Batch
                </h5>
            </div>
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-hover mb-0">
                        <thead class="table-light">
                            <tr>
                                <th style="width: 50px;">#</th>
                                <th>Filename</th>
                                <th style="width: 100px;">Pages</th>
                                <th style="width: 150px;">Status</th>
                                <th style="width: 200px;">Actions</th>
                            </tr>
                        </thead>
                        <tbody id="documentsTable">
                            <c:forEach items="${batchJob.documents}" var="doc" varStatus="status">
                                <tr class="doc-item status-${doc.status.toString().toLowerCase()}" 
                                    data-doc-id="${doc.id}">
                                    <td>${status.index + 1}</td>
                                    <td>
                                        <i class="fas fa-file-pdf text-danger"></i>
                                        ${doc.filename}
                                    </td>
                                    <td>${doc.totalPages != null ? doc.totalPages : '-'}</td>
                                    <td>
                                        <span class="status-indicator status-${doc.status.toString().toLowerCase()}"></span>
                                        <span class="doc-status">${doc.status}</span>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${doc.status == 'ANALYZED'}">
                                                <a href="/analyze/${doc.id}" class="btn btn-sm btn-success">
                                                    <i class="fas fa-eye"></i> View
                                                </a>
                                            </c:when>
                                            <c:when test="${doc.status == 'ANALYZING' or doc.status == 'PROCESSING'}">
                                                <span class="badge bg-warning">
                                                    <i class="fas fa-spinner fa-spin"></i> Processing
                                                </span>
                                            </c:when>
                                            <c:when test="${doc.status == 'FAILED'}">
                                                <button class="btn btn-sm btn-outline-danger" 
                                                        onclick="showError('${doc.id}')">
                                                    <i class="fas fa-exclamation-circle"></i> View Error
                                                </button>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge bg-secondary">${doc.status}</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

    <script src="/webjars/jquery/3.7.1/jquery.min.js"></script>
    <script src="/webjars/bootstrap/5.3.0/js/bootstrap.bundle.min.js"></script>
    <script>
        const batchJobId = '${batchJob.id}';
        const isRunning = '${batchJob.status}' === 'RUNNING';
        
        // Auto-refresh if job is running
        if (isRunning) {
            setInterval(updateProgress, 3000); // Update every 3 seconds
        }
        
        async function updateProgress() {
            try {
                const response = await fetch(`/api/batch/${batchJobId}/summary`);
                const data = await response.json();
                
                // Update progress
                document.getElementById('processedCount').textContent = data.processedCount;
                document.getElementById('progressPercent').textContent = data.progressPercentage.toFixed(1);
                
                const progressBar = document.getElementById('mainProgressBar');
                progressBar.style.width = data.progressPercentage + '%';
                
                // Update statistics
                document.getElementById('successDocs').textContent = data.successCount;
                document.getElementById('failedDocs').textContent = data.failureCount;
                document.getElementById('remainingDocs').textContent = 
                    data.totalDocuments - data.processedCount;
                
                // Update ETA
                if (data.estimatedTimeRemaining) {
                    const minutes = Math.floor(data.estimatedTimeRemaining / 60);
                    const seconds = data.estimatedTimeRemaining % 60;
                    document.getElementById('eta').textContent = 
                        `${minutes}m ${seconds}s`;
                }
                
                // Update document statuses
                data.documents.forEach(doc => {
                    const row = document.querySelector(`tr[data-doc-id="${doc.filename}"]`);
                    if (row) {
                        const statusCell = row.querySelector('.doc-status');
                        if (statusCell) {
                            statusCell.textContent = doc.status;
                        }
                    }
                });
                
                // Reload page if completed
                if (data.status === 'COMPLETED' || data.status === 'FAILED') {
                    setTimeout(() => location.reload(), 2000);
                }
                
            } catch (error) {
                console.error('Error updating progress:', error);
            }
        }
        
        function exportToExcel() {
            window.location.href = `/api/batch/${batchJobId}/export/excel`;
        }
        
        function exportToCSV() {
            window.location.href = `/api/batch/${batchJobId}/export/csv`;
        }
        
        function pauseJob() {
            if (confirm('Pause this batch job?')) {
                fetch(`/api/batch/${batchJobId}/pause`, { method: 'POST' })
                    .then(() => location.reload());
            }
        }
        
        function cancelJob() {
            if (confirm('Cancel this batch job? This cannot be undone.')) {
                fetch(`/api/batch/${batchJobId}/cancel`, { method: 'POST' })
                    .then(() => location.reload());
            }
        }
        
        function showError(docId) {
            // Fetch and display error details
            fetch(`/api/documents/${docId}`)
                .then(response => response.json())
                .then(doc => {
                    alert('Error: ' + doc.aiAnalysis);
                });
        }
    </script>
</body>
</html>