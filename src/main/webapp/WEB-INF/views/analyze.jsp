<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <title>Document AI - Analyze</title>
    <link rel="stylesheet" href="/webjars/bootstrap/5.3.0/css/bootstrap.min.css">
    <style>
        .analysis-result {
            background: #f8f9fa;
            border-left: 4px solid #0d6efd;
            padding: 20px;
            border-radius: 5px;
            white-space: pre-wrap;
            font-family: 'Courier New', monospace;
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
                <a class="nav-link" href="/documents">Documents</a>
            </div>
        </div>
    </nav>

    <div class="container mt-5">
        <div class="row">
            <div class="col-md-10 offset-md-1">
                <h1>🤖 AI Analysis</h1>
                
                <!-- Document Info -->
                <div class="card mb-4">
                    <div class="card-body">
                        <h5 class="card-title">📄 ${document.filename}</h5>
                        <p class="card-text">
                            <strong>Status:</strong> 
                            <span class="badge bg-${document.status == 'ANALYZED' ? 'success' : 'info'}">
                                ${document.status}
                            </span>
                        </p>
                    </div>
                </div>

                <c:choose>
                    <c:when test="${document.status == 'ANALYZED'}">
                        <!-- Show Analysis Result -->
                        <h3>✅ Analysis Result:</h3>
                        <div class="analysis-result mb-4">
                            ${document.aiAnalysis}
                        </div>
                        
                        <div class="d-flex gap-2">
                            <a href="/documents" class="btn btn-secondary">← Back to Documents</a>
                            <a href="/analyze/${document.id}" class="btn btn-primary">🔄 Analyze Again</a>
                        </div>
                    </c:when>
                    
                    <c:otherwise>
                        <!-- Analysis Form -->
                        <form action="/analyze/${document.id}" method="post">
                            <div class="mb-3">
                                <label for="prompt" class="form-label">
                                    <strong>What would you like to analyze?</strong>
                                </label>
                                <textarea class="form-control" 
                                          id="prompt" 
                                          name="prompt" 
                                          rows="4" 
                                          required
                                          placeholder="Example: Find the capital commitment amount and lock-in period from this document."></textarea>
                                <div class="form-text">
                                    Be specific about what you want to extract or analyze.
                                </div>
                            </div>

                            <!-- Preset Prompts -->
                            <div class="mb-3">
                                <label class="form-label"><strong>Or use a preset:</strong></label>
                                <div class="d-grid gap-2">
                                    <button type="button" class="btn btn-outline-primary btn-sm text-start"
                                            onclick="document.getElementById('prompt').value='Find and extract the capital commitment amount mentioned in this document.'">
                                        💰 Extract Capital Commitment
                                    </button>
                                    <button type="button" class="btn btn-outline-primary btn-sm text-start"
                                            onclick="document.getElementById('prompt').value='Identify all key dates, deadlines, and time periods mentioned in this document.'">
                                        📅 Extract Dates & Deadlines
                                    </button>
                                    <button type="button" class="btn btn-outline-primary btn-sm text-start"
                                            onclick="document.getElementById('prompt').value='Summarize the main terms and conditions of this agreement in bullet points.'">
                                        📋 Summarize Terms
                                    </button>
                                    <button type="button" class="btn btn-outline-primary btn-sm text-start"
                                            onclick="document.getElementById('prompt').value='Extract all financial figures, fees, percentages, and monetary amounts.'">
                                        💵 Extract Financial Details
                                    </button>
                                </div>
                            </div>

                            <div class="alert alert-info">
                                <strong>⏱️ Processing Time:</strong> Analysis typically takes 20-30 seconds.
                                You'll be redirected when complete.
                            </div>

                            <div class="d-flex gap-2">
                                <a href="/documents" class="btn btn-secondary">← Cancel</a>
                                <button type="submit" class="btn btn-primary btn-lg">
                                    🚀 Start AI Analysis
                                </button>
                            </div>
                        </form>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>

    <script src="/webjars/jquery/3.7.1/jquery.min.js"></script>
    <script src="/webjars/bootstrap/5.3.0/js/bootstrap.bundle.min.js"></script>
</body>
</html>