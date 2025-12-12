<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <title>Batch Upload - Document AI</title>
    <link rel="stylesheet" href="/webjars/bootstrap/5.3.0/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        .drop-zone {
            border: 3px dashed #0d6efd;
            border-radius: 10px;
            padding: 60px;
            text-align: center;
            background: #f8f9fa;
            cursor: pointer;
            transition: all 0.3s;
            min-height: 300px;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
        }
        .drop-zone:hover, .drop-zone.drag-over {
            background: #e9ecef;
            border-color: #0a58ca;
            transform: scale(1.02);
        }
        .file-list {
            max-height: 400px;
            overflow-y: auto;
        }
        .file-item {
            background: white;
            border: 1px solid #dee2e6;
            padding: 10px 15px;
            margin-bottom: 10px;
            border-radius: 5px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .file-item:hover {
            background: #f8f9fa;
        }
        .upload-progress {
            display: none;
        }
        .upload-progress.show {
            display: block;
        }
    </style>
</head>
<body>
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary">
        <div class="container">
            <a class="navbar-brand" href="/"><strong>📄 Document AI Analyzer</strong></a>
            <div class="navbar-nav">
                <a class="nav-link" href="/">Upload</a>
                <a class="nav-link" href="/documents">Documents</a>
                <a class="nav-link active" href="/batch">Batch Upload</a>
                <a class="nav-link" href="/batch/jobs">Batch Jobs</a>
            </div>
        </div>
    </nav>

    <div class="container mt-5">
        <div class="row">
            <div class="col-md-10 offset-md-1">
                <h1 class="mb-4"><i class="fas fa-layer-group"></i> Batch Upload & Processing</h1>
                
                <div class="alert alert-info">
                    <i class="fas fa-info-circle"></i>
                    <strong>Batch Processing:</strong> Upload multiple PDF documents (up to 1000) and process them all automatically with AI analysis.
                </div>

                <div class="card mb-4">
                    <div class="card-header bg-primary text-white">
                        <h5 class="mb-0"><i class="fas fa-cog"></i> Job Configuration</h5>
                    </div>
                    <div class="card-body">
                        <div class="mb-3">
                            <label for="jobName" class="form-label">Job Name *</label>
                            <input type="text" class="form-control" id="jobName" placeholder="e.g., Q4 2024 Fund Agreements" required>
                        </div>
                        <div class="mb-3">
                            <label for="jobDescription" class="form-label">Description (Optional)</label>
                            <textarea class="form-control" id="jobDescription" rows="2" placeholder="Brief description of this batch job"></textarea>
                        </div>
                        <div class="mb-3">
                            <label for="analysisTemplate" class="form-label">Analysis Prompt (Optional)</label>
                            <textarea class="form-control" id="analysisTemplate" rows="3" placeholder="e.g., Extract contributor name, capital commitment, PAN number, and lock-in period from each document."></textarea>
                            <div class="form-text">This prompt will be applied to ALL documents in the batch. Leave empty to skip AI analysis.</div>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Quick Templates:</label>
                            <div class="d-grid gap-2">
                                <button class="btn btn-sm btn-outline-secondary text-start" onclick="setTemplate('fund')">📊 Fund Agreement Analysis</button>
                                <button class="btn btn-sm btn-outline-secondary text-start" onclick="setTemplate('extract')">📋 Basic Data Extraction</button>
                                <button class="btn btn-sm btn-outline-secondary text-start" onclick="setTemplate('compliance')">✅ Compliance Check</button>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="card mb-4">
                    <div class="card-header bg-success text-white">
                        <h5 class="mb-0"><i class="fas fa-upload"></i> Upload Documents</h5>
                    </div>
                    <div class="card-body">
                        <div class="drop-zone" id="dropZone">
                            <i class="fas fa-cloud-upload-alt fa-4x text-primary mb-3"></i>
                            <h4>Drag & Drop Files Here</h4>
                            <p class="text-muted">or click to browse</p>
                            <p class="text-muted"><small>Supports: PDF files only • Max 50MB per file • Up to 1000 files</small></p>
                            <input type="file" id="fileInput" multiple accept=".pdf" style="display: none;">
                        </div>
                        <div id="fileListContainer" style="display: none;" class="mt-4">
                            <h5>Selected Files: <span id="fileCount" class="badge bg-primary">0</span></h5>
                            <div class="file-list" id="fileList"></div>
                        </div>
                    </div>
                </div>

                <div class="d-flex gap-2 mb-4">
                    <button class="btn btn-primary btn-lg" id="uploadBtn" disabled onclick="startBatchUpload()">
                        <i class="fas fa-rocket"></i> Start Batch Processing
                    </button>
                    <button class="btn btn-secondary btn-lg" onclick="clearFiles()">
                        <i class="fas fa-trash"></i> Clear All
                    </button>
                    <a href="/batch/jobs" class="btn btn-outline-primary btn-lg">
                        <i class="fas fa-list"></i> View All Jobs
                    </a>
                </div>

                <div class="upload-progress" id="uploadProgress">
                    <div class="card">
                        <div class="card-header bg-warning">
                            <h5 class="mb-0"><i class="fas fa-spinner fa-spin"></i> Uploading & Processing...</h5>
                        </div>
                        <div class="card-body">
                            <div class="progress mb-3" style="height: 30px;">
                                <div class="progress-bar progress-bar-striped progress-bar-animated" id="progressBar" style="width: 0%">0%</div>
                            </div>
                            <p id="progressText">Uploading files...</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="/webjars/jquery/3.7.1/jquery.min.js"></script>
    <script src="/webjars/bootstrap/5.3.0/js/bootstrap.bundle.min.js"></script>
    <script>
        let selectedFiles = [];
        
        const templates = {
            fund: 'Extract the following from each fund agreement:\n1. Contributor/Investor Name\n2. Capital Commitment Amount\n3. PAN Number\n4. Lock-in Period\n5. Management Fee\n6. Carried Interest\n7. Agreement Date\n\nFormat as structured data.',
            extract: 'Extract all key information including:\n- Names and roles\n- Financial amounts\n- Dates\n- Identification numbers (PAN, Aadhar)\n- Contact information',
            compliance: 'Check for the following:\n1. Is PAN number present and valid?\n2. Is signature present?\n3. Is date filled in?\n4. Are all mandatory fields completed?\n5. Any discrepancies or issues?'
        };
        
        function setTemplate(type) {
            document.getElementById('analysisTemplate').value = templates[type];
        }
        
        const dropZone = document.getElementById('dropZone');
        const fileInput = document.getElementById('fileInput');
        
        dropZone.addEventListener('click', () => fileInput.click());
        dropZone.addEventListener('dragover', (e) => { e.preventDefault(); dropZone.classList.add('drag-over'); });
        dropZone.addEventListener('dragleave', () => dropZone.classList.remove('drag-over'));
        dropZone.addEventListener('drop', (e) => { e.preventDefault(); dropZone.classList.remove('drag-over'); handleFiles(e.dataTransfer.files); });
        fileInput.addEventListener('change', (e) => handleFiles(e.target.files));
        
        function handleFiles(files) {
            selectedFiles = Array.from(files).filter(f => f.name.toLowerCase().endsWith('.pdf'));
            if (selectedFiles.length === 0) { alert('Please select PDF files only'); return; }
            if (selectedFiles.length > 1000) { alert('Maximum 1000 files allowed'); selectedFiles = selectedFiles.slice(0, 1000); }
            updateFileList();
        }
        
        function updateFileList() {
            const fileList = document.getElementById('fileList');
            const fileCount = document.getElementById('fileCount');
            const uploadBtn = document.getElementById('uploadBtn');
            
            fileCount.textContent = selectedFiles.length;
            
            if (selectedFiles.length > 0) {
                document.getElementById('fileListContainer').style.display = 'block';
                uploadBtn.disabled = false;
                fileList.innerHTML = selectedFiles.map((file, index) => 
                    '<div class="file-item"><div><i class="fas fa-file-pdf text-danger"></i> <strong>' + file.name + '</strong> <span class="text-muted">(' + (file.size / 1024 / 1024).toFixed(2) + ' MB)</span></div><button class="btn btn-sm btn-outline-danger" onclick="removeFile(' + index + ')"><i class="fas fa-times"></i></button></div>'
                ).join('');
            } else {
                document.getElementById('fileListContainer').style.display = 'none';
                uploadBtn.disabled = true;
            }
        }
        
        function removeFile(index) { selectedFiles.splice(index, 1); updateFileList(); }
        function clearFiles() { selectedFiles = []; fileInput.value = ''; updateFileList(); }
        
        async function startBatchUpload() {
            const jobName = document.getElementById('jobName').value.trim();
            if (!jobName) { alert('Please enter a job name'); return; }
            if (selectedFiles.length === 0) { alert('Please select files'); return; }
            
            document.getElementById('uploadProgress').classList.add('show');
            document.getElementById('uploadBtn').disabled = true;
            
            const formData = new FormData();
            formData.append('jobName', jobName);
            formData.append('description', document.getElementById('jobDescription').value);
            formData.append('template', document.getElementById('analysisTemplate').value);
            selectedFiles.forEach(file => formData.append('files', file));
            
            try {
                document.getElementById('progressText').textContent = 'Uploading files...';
                document.getElementById('progressBar').style.width = '25%';
                document.getElementById('progressBar').textContent = '25%';
                
                const uploadResponse = await fetch('/api/batch/upload', { method: 'POST', body: formData });
                if (!uploadResponse.ok) throw new Error('Upload failed: ' + await uploadResponse.text());
                
                const batchJob = await uploadResponse.json();
                if (!batchJob || !batchJob.id) throw new Error('Invalid response from server - no batch job ID');
                
                console.log('Batch Job Created:', batchJob);
                
                document.getElementById('progressText').textContent = 'Starting batch processing...';
                document.getElementById('progressBar').style.width = '50%';
                document.getElementById('progressBar').textContent = '50%';
                
                const startResponse = await fetch('/api/batch/' + batchJob.id + '/start', { method: 'POST' });
                if (!startResponse.ok) throw new Error('Failed to start processing: ' + await startResponse.text());
                
                document.getElementById('progressText').textContent = 'Redirecting...';
                document.getElementById('progressBar').style.width = '100%';
                document.getElementById('progressBar').textContent = '100%';
                
                setTimeout(() => window.location.href = '/batch/' + batchJob.id, 1500);
                
            } catch (error) {
                console.error('Batch upload error:', error);
                alert('Error: ' + error.message);
                document.getElementById('uploadProgress').classList.remove('show');
                document.getElementById('uploadBtn').disabled = false;
            }
        }
    </script>
</body>
</html>