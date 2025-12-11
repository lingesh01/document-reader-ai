<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <title>Document AI - Documents</title>
    <link rel="stylesheet" href="/webjars/bootstrap/5.3.0/css/bootstrap.min.css">
    <style>
        .status-badge {
            font-size: 0.9em;
            padding: 5px 10px;
        }
        .card:hover {
            box-shadow: 0 4px 8px rgba(0,0,0,0.1);
            transition: all 0.3s;
        }
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
                <a class="nav-link active" href="/documents">Documents</a>
            </div>
        </div>
    </nav>

    <div class="container mt-5">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h1>📚 Your Documents</h1>
            <a href="/" class="btn btn-primary">➕ Upload New</a>
        </div>

        <!-- Success/Error Messages -->
        <c:if test="${not empty success}">
            <div class="alert alert-success alert-dismissible fade show">
                ${success}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </c:if>

        <c:if test="${not empty error}">
            <div class="alert alert-danger alert-dismissible fade show">
                ${error}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </c:if>

        <!-- Documents List -->
        <c:choose>
            <c:when test="${empty documents}">
                <div class="alert alert-info text-center">
                    <h4>No documents yet!</h4>
                    <p>Upload your first document to get started.</p>
                    <a href="/" class="btn btn-primary">Upload Document</a>
                </div>
            </c:when>
            <c:otherwise>
                <div class="row">
                    <c:forEach items="${documents}" var="doc">
                        <div class="col-md-6 col-lg-4 mb-4">
                            <div class="card h-100">
                                <div class="card-body">
                                    <h5 class="card-title">
                                        📄 ${doc.filename}
                                    </h5>
                                    
                                    <p class="card-text text-muted">
                                        <small>
                                            <strong>Size:</strong> 
                                            <fmt:formatNumber value="${doc.fileSize / 1024}" maxFractionDigits="2"/> KB<br>
                                            <strong>Pages:</strong> ${doc.totalPages != null ? doc.totalPages : 'N/A'}<br>
                                            <strong>Uploaded:</strong> 
                                            ${doc.createdAt.toString().substring(0, 19).replace('T', ' ')}
                                        </small>
                                    </p>

                                    <!-- Status Badge -->
                                    <c:choose>
                                        <c:when test="${doc.status == 'ANALYZED'}">
                                            <span class="badge bg-success status-badge">✓ Analyzed</span>
                                        </c:when>
                                      
										
										<c:when test="${doc.status == 'ANALYZING'}">
										    <span class="badge bg-warning status-badge">⏳ Analyzing...</span>
										    <div class="mt-2">
										        <a href="/documents?analyzing=${doc.id}" 
										           class="btn btn-sm btn-warning w-100">
										            👁️ Watch Progress
										        </a>
										    </div>
										</c:when>
										
										
                                        <c:when test="${doc.status == 'READY'}">
                                            <span class="badge bg-info status-badge">✓ Ready</span>
                                        </c:when>
                                        <c:when test="${doc.status == 'FAILED'}">
                                            <span class="badge bg-danger status-badge">✗ Failed</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge bg-secondary status-badge">${doc.status}</span>
                                        </c:otherwise>
                                    </c:choose>

                                    <!-- Actions -->
                                    <div class="mt-3">
                                        <c:choose>
                                            <c:when test="${doc.status == 'ANALYZED'}">
                                                <a href="/analyze/${doc.id}" class="btn btn-sm btn-success w-100">
                                                    👁️ View Analysis
                                                </a>
                                            </c:when>
                                            <c:when test="${doc.status == 'ANALYZING'}">
                                                <button class="btn btn-sm btn-warning w-100" 
                                                        onclick="location.reload()">
                                                    🔄 Refresh Status
                                                </button>
                                            </c:when>
                                            <c:when test="${doc.status == 'READY'}">
                                                <a href="/analyze/${doc.id}" class="btn btn-sm btn-primary w-100">
                                                    🤖 Analyze with AI
                                                </a>
                                            </c:when>
                                            <c:otherwise>
                                                <button class="btn btn-sm btn-secondary w-100" disabled>
                                                    Processing...
                                                </button>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
	
	
	<!-- Analysis Status Modal -->
	   <div class="modal fade" id="statusModal" tabindex="-1" data-bs-backdrop="static" data-bs-keyboard="false">
	       <div class="modal-dialog modal-dialog-centered">
	           <div class="modal-content">
	               <div class="modal-header">
	                   <h5 class="modal-title">🤖 AI Analysis in Progress</h5>
	               </div>
	               <div class="modal-body text-center">
	                   <div class="spinner-border text-primary mb-3" role="status" style="width: 3rem; height: 3rem;">
	                       <span class="visually-hidden">Loading...</span>
	                   </div>
	                   <h5 id="statusMessage">Analyzing document with Mistral AI...</h5>
	                   <p class="text-muted mb-0" id="statusDetail">This usually takes 20-30 seconds</p>
	                   <div class="progress mt-3" style="height: 25px;">
	                       <div class="progress-bar progress-bar-striped progress-bar-animated" 
	                            role="progressbar" 
	                            style="width: 0%" 
	                            id="progressBar">0%</div>
	                   </div>
	               </div>
	           </div>
	       </div>
	   </div>

	   <script src="/webjars/jquery/3.7.1/jquery.min.js"></script>
	      <script src="/webjars/bootstrap/5.3.0/js/bootstrap.bundle.min.js"></script>
	      
	      <!-- Status Checking Script -->
	      <script>
	          var checkInterval;
	          var progressValue = 0;
	          
			  function checkDocumentStatus(documentId) {
			      $.ajax({
			          url: '/api/documents/' + documentId + '/status',
			          method: 'GET',
			          success: function(data) {
			              console.log('Status:', data);
			              
			              if (data.status === 'ANALYZING') {
			                  // Still processing
			                  $('#statusMessage').text('AI is analyzing your document...');
			                  $('#statusDetail').text('Mistral is processing the text');
			                  
			                  // Fake progress (just for UX)
			                  progressValue = Math.min(progressValue + 5, 90);
			                  $('#progressBar').css('width', progressValue + '%').text(progressValue + '%');
			                  
			              } else if (data.status === 'ANALYZED') {
			                  // Complete!
			                  $('#statusMessage').text('✅ Analysis Complete!');
			                  $('#statusDetail').text('Redirecting...');
			                  $('#progressBar').removeClass('progress-bar-animated')
			                                  .addClass('bg-success')
			                                  .css('width', '100%')
			                                  .text('100%');
			                  
			                  clearInterval(checkInterval);  // ← STOP POLLING
			                  
			                  // Redirect to analysis page
			                  setTimeout(function() {
			                      window.location.href = '/analyze/' + documentId;
			                  }, 1500);
			                  
			              } else if (data.status === 'FAILED') {
			                  // Error
			                  $('#statusMessage').text('❌ Analysis Failed');
			                  $('#statusDetail').text(data.error || 'An error occurred');
			                  $('#progressBar').removeClass('progress-bar-animated')
			                                  .addClass('bg-danger')
			                                  .css('width', '100%')
			                                  .text('Error');
			                  
			                  clearInterval(checkInterval);  // ← STOP POLLING
			                  
			                  setTimeout(function() {
			                      location.reload();
			                  }, 3000);
			              } else {
			                  // Unknown status - stop polling
			                  console.log('Unknown status:', data.status);
			                  clearInterval(checkInterval);  // ← ADD THIS
			                  location.reload();
			              }
			          },
			          error: function(xhr, status, error) {
			              console.error('Error checking status:', error);
			              // Don't stop polling on error - might be temporary
			          }
			      });
			  }
	          
	          $(document).ready(function() {
	              // Check URL for documentId parameter
	              const urlParams = new URLSearchParams(window.location.search);
	              const analyzingDocId = urlParams.get('analyzing');
	              
	              if (analyzingDocId) {
	                  // Show modal
	                  var modal = new bootstrap.Modal(document.getElementById('statusModal'));
	                  modal.show();
	                  
	                  // Start checking status
	                  progressValue = 10;
	                  $('#progressBar').css('width', '10%').text('10%');
	                  
	                  checkInterval = setInterval(function() {
	                      checkDocumentStatus(analyzingDocId);
	                  }, 2000); // Check every 2 seconds
	              }
	              
	              // Auto-refresh if any document is analyzing (fallback)
	              var hasAnalyzing = false;
	              <c:forEach items="${documents}" var="doc">
	                  if ('${doc.status}' === 'ANALYZING') {
	                      hasAnalyzing = true;
	                  }
	              </c:forEach>
	              
	              if (hasAnalyzing && !analyzingDocId) {
	                  setTimeout(function() {
	                      location.reload();
	                  }, 5000);
	              }
	          });
	      </script>
</body>
</html>