<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <title>Document Viewer - AI Analyzer</title>
    <link rel="stylesheet" href="/webjars/bootstrap/5.3.0/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        body {
            overflow: hidden;
        }
        
        .viewer-container {
            height: 100vh;
            display: flex;
            flex-direction: column;
        }
        
        .viewer-header {
            background: #2c3e50;
            color: white;
            padding: 15px 20px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        
        .viewer-content {
            flex: 1;
            display: flex;
            overflow: hidden;
        }
        
        .pdf-panel {
            flex: 1;
            background: #ecf0f1;
            display: flex;
            flex-direction: column;
            overflow: hidden;
        }
        
        .pdf-toolbar {
            background: white;
            padding: 10px;
            border-bottom: 1px solid #ddd;
            display: flex;
            gap: 10px;
            align-items: center;
        }
        
        .pdf-viewer {
            flex: 1;
            overflow-y: auto;
            padding: 20px;
            text-align: center;
        }
        
        .page-thumbnail {
            background: white;
            border: 2px solid #ddd;
            border-radius: 4px;
            padding: 10px;
            margin-bottom: 20px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            cursor: pointer;
            transition: all 0.3s;
        }
        
        .page-thumbnail:hover {
            border-color: #3498db;
            box-shadow: 0 4px 8px rgba(0,0,0,0.2);
        }
        
        .page-thumbnail.active {
            border-color: #2ecc71;
            box-shadow: 0 4px 12px rgba(46,204,113,0.4);
        }
        
        .page-number {
            background: #3498db;
            color: white;
            padding: 5px 10px;
            border-radius: 20px;
            font-size: 12px;
            margin-bottom: 10px;
            display: inline-block;
        }
        
        .analysis-panel {
            width: 45%;
            background: white;
            border-left: 1px solid #ddd;
            display: flex;
            flex-direction: column;
            overflow: hidden;
        }
        
        .analysis-panel.collapsed {
            width: 0;
            border: none;
        }
        
        .analysis-header {
            background: #34495e;
            color: white;
            padding: 15px 20px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        
        .analysis-content {
            flex: 1;
            overflow-y: auto;
            padding: 20px;
        }
        
        .analysis-section {
            background: #f8f9fa;
            border-left: 4px solid #3498db;
            padding: 15px;
            margin-bottom: 20px;
            border-radius: 4px;
        }
        
        .analysis-section h5 {
            color: #2c3e50;
            margin-bottom: 10px;
        }
        
        .extracted-value {
            background: #fff3cd;
            padding: 5px 10px;
            border-radius: 3px;
            font-family: monospace;
            margin: 5px 0;
        }
        
        .ai-prompt-bar {
            background: white;
            padding: 15px 20px;
            border-top: 1px solid #ddd;
        }
        
        .resizer {
            width: 5px;
            background: #bdc3c7;
            cursor: col-resize;
            position: relative;
        }
        
        .resizer:hover {
            background: #3498db;
        }
        
        .btn-icon {
            padding: 8px 15px;
            border-radius: 4px;
        }
        
        .highlight {
            background-color: yellow;
            padding: 2px 4px;
            border-radius: 2px;
        }
        
        .loading-overlay {
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(0,0,0,0.7);
            display: none;
            justify-content: center;
            align-items: center;
            z-index: 9999;
        }
        
        .loading-overlay.show {
            display: flex;
        }
        
        .spinner {
            width: 60px;
            height: 60px;
            border: 5px solid #f3f3f3;
            border-top: 5px solid #3498db;
            border-radius: 50%;
            animation: spin 1s linear infinite;
        }
        
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
    </style>
</head>
<body>
    <!-- Loading Overlay -->
    <div class="loading-overlay" id="loadingOverlay">
        <div class="text-center">
            <div class="spinner"></div>
            <p class="text-white mt-3">Analyzing document...</p>
        </div>
    </div>

    <div class="viewer-container">
        <!-- Header -->
        <div class="viewer-header">
            <div>
                <h5 class="mb-0">
                    <i class="fas fa-file-pdf"></i>
                    ${document.filename}
                </h5>
                <small>${document.totalPages} pages • ${document.fileSize / 1024} KB</small>
            </div>
            <div>
                <button class="btn btn-light btn-sm me-2" onclick="exportAnalysis()">
                    <i class="fas fa-download"></i> Export
                </button>
                <button class="btn btn-light btn-sm me-2" onclick="togglePanel()">
                    <i class="fas fa-columns"></i> Toggle View
                </button>
                <a href="/documents" class="btn btn-secondary btn-sm">
                    <i class="fas fa-times"></i> Close
                </a>
            </div>
        </div>

        <!-- Main Content -->
        <div class="viewer-content">
            <!-- PDF Panel -->
            <div class="pdf-panel">
                <div class="pdf-toolbar">
                    <button class="btn btn-sm btn-outline-secondary" onclick="zoomOut()">
                        <i class="fas fa-search-minus"></i>
                    </button>
                    <span id="zoomLevel">100%</span>
                    <button class="btn btn-sm btn-outline-secondary" onclick="zoomIn()">
                        <i class="fas fa-search-plus"></i>
                    </button>
                    <div class="ms-auto">
                        <span class="badge bg-primary">${document.status}</span>
                    </div>
                </div>
                
                <div class="pdf-viewer" id="pdfViewer">
                    <!-- Page thumbnails will be loaded here -->
                    <c:forEach begin="1" end="${document.totalPages}" var="pageNum">
                        <div class="page-thumbnail" id="page${pageNum}" onclick="selectPage(${pageNum})">
                            <div class="page-number">Page ${pageNum}</div>
                            <div class="page-content" style="white-space: pre-wrap; text-align: left; font-size: 12px; max-height: 400px; overflow: hidden;">
                                <!-- Page content preview -->
                                <div style="background: #f8f9fa; padding: 15px; border-radius: 4px;">
                                    <i class="fas fa-file-alt text-muted fa-3x"></i>
                                    <p class="text-muted mt-2 mb-0">Page ${pageNum}</p>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </div>

            <!-- Resizer -->
            <div class="resizer" id="resizer"></div>

            <!-- Analysis Panel -->
            <div class="analysis-panel" id="analysisPanel">
                <div class="analysis-header">
                    <h5 class="mb-0">
                        <i class="fas fa-brain"></i>
                        AI Analysis
                    </h5>
                    <button class="btn btn-sm btn-outline-light" onclick="refreshAnalysis()">
                        <i class="fas fa-sync"></i>
                    </button>
                </div>

                <div class="analysis-content" id="analysisContent">
                    <c:choose>
                        <c:when test="${document.status == 'ANALYZED'}">
                            <!-- Display analysis results -->
                            <div class="analysis-section">
                                <h5><i class="fas fa-info-circle"></i> Analysis Results</h5>
                                <div style="white-space: pre-wrap; font-family: 'Courier New', monospace;">
                                    ${document.aiAnalysis}
                                </div>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="alert alert-info">
                                <i class="fas fa-lightbulb"></i>
                                <strong>Ready to analyze!</strong>
                                <p class="mb-0 mt-2">Enter your question below to get AI-powered insights from this document.</p>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>

                <!-- AI Prompt Bar -->
                <div class="ai-prompt-bar">
                    <form onsubmit="return analyzeDocument(event)">
                        <div class="input-group">
                            <input type="text" 
                                   class="form-control" 
                                   id="promptInput"
                                   placeholder="Ask anything about this document..."
                                   required>
                            <button class="btn btn-primary" type="submit">
                                <i class="fas fa-paper-plane"></i> Analyze
                            </button>
                        </div>
                        <div class="mt-2">
                            <small class="text-muted">
                                Quick prompts:
                                <a href="#" onclick="setPrompt('Extract all names and PAN numbers'); return false;">Names & PAN</a> |
                                <a href="#" onclick="setPrompt('Find all financial amounts'); return false;">Amounts</a> |
                                <a href="#" onclick="setPrompt('Extract key dates'); return false;">Dates</a> |
                                <a href="#" onclick="setPrompt('Summarize tables'); return false;">Tables</a>
                            </small>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>

    <script src="/webjars/jquery/3.7.1/jquery.min.js"></script>
    <script src="/webjars/bootstrap/5.3.0/js/bootstrap.bundle.min.js"></script>
    <script>
        let currentZoom = 100;
        const documentId = '${document.id}';

        function zoomIn() {
            currentZoom = Math.min(200, currentZoom + 10);
            updateZoom();
        }

        function zoomOut() {
            currentZoom = Math.max(50, currentZoom - 10);
            updateZoom();
        }

        function updateZoom() {
            document.getElementById('zoomLevel').textContent = currentZoom + '%';
            document.getElementById('pdfViewer').style.transform = `scale(${currentZoom / 100})`;
        }

        function selectPage(pageNum) {
            // Remove active class from all pages
            document.querySelectorAll('.page-thumbnail').forEach(p => p.classList.remove('active'));
            // Add active class to selected page
            document.getElementById('page' + pageNum).classList.add('active');
            // Scroll to page
            document.getElementById('page' + pageNum).scrollIntoView({ behavior: 'smooth', block: 'center' });
        }

        function togglePanel() {
            document.getElementById('analysisPanel').classList.toggle('collapsed');
        }

        function setPrompt(text) {
            document.getElementById('promptInput').value = text;
        }

        function analyzeDocument(event) {
            event.preventDefault();
            
            const prompt = document.getElementById('promptInput').value;
            
            // Show loading
            document.getElementById('loadingOverlay').classList.add('show');
            
            // Send analysis request
            fetch(`/api/documents/${documentId}/analyze`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ prompt: prompt })
            })
            .then(response => response.json())
            .then(data => {
                // Poll for status
                checkAnalysisStatus();
            })
            .catch(error => {
                console.error('Error:', error);
                document.getElementById('loadingOverlay').classList.remove('show');
                alert('Analysis failed: ' + error.message);
            });
            
            return false;
        }

        function checkAnalysisStatus() {
            fetch(`/api/documents/${documentId}/status`)
                .then(response => response.json())
                .then(data => {
                    if (data.status === 'ANALYZED') {
                        // Analysis complete, reload page
                        location.reload();
                    } else if (data.status === 'FAILED') {
                        document.getElementById('loadingOverlay').classList.remove('show');
                        alert('Analysis failed: ' + data.error);
                    } else {
                        // Still processing, check again
                        setTimeout(checkAnalysisStatus, 2000);
                    }
                });
        }

        function refreshAnalysis() {
            location.reload();
        }

        function exportAnalysis() {
            // Create exportable content
            const analysis = document.getElementById('analysisContent').innerText;
            const blob = new Blob([analysis], { type: 'text/plain' });
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = '${document.filename}-analysis.txt';
            a.click();
        }

        // Make resizer draggable
        const resizer = document.getElementById('resizer');
        const pdfPanel = document.querySelector('.pdf-panel');
        const analysisPanel = document.getElementById('analysisPanel');

        let isResizing = false;

        resizer.addEventListener('mousedown', (e) => {
            isResizing = true;
            document.body.style.cursor = 'col-resize';
        });

        document.addEventListener('mousemove', (e) => {
            if (!isResizing) return;

            const containerWidth = document.querySelector('.viewer-content').offsetWidth;
            const pdfWidth = (e.clientX / containerWidth) * 100;

            if (pdfWidth > 30 && pdfWidth < 70) {
                pdfPanel.style.flex = `0 0 ${pdfWidth}%`;
                analysisPanel.style.width = `${100 - pdfWidth}%`;
            }
        });

        document.addEventListener('mouseup', () => {
            isResizing = false;
            document.body.style.cursor = 'default';
        });
    </script>
</body>
</html>