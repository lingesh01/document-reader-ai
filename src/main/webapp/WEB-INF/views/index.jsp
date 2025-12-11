<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <title>Document AI - Upload</title>
    <link rel="stylesheet" href="/webjars/bootstrap/5.3.0/css/bootstrap.min.css">
    <style>
        .upload-area {
            border: 3px dashed #0d6efd;
            border-radius: 10px;
            padding: 60px;
            text-align: center;
            background: #f8f9fa;
            cursor: pointer;
            transition: all 0.3s;
        }
        .upload-area:hover {
            background: #e9ecef;
            border-color: #0a58ca;
        }
        .upload-area i {
            font-size: 48px;
            color: #0d6efd;
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
            <div class="col-md-8 offset-md-2">
                <h1 class="text-center mb-4">Upload Document</h1>
                
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

                <!-- Upload Form -->
                <form action="/upload" method="post" enctype="multipart/form-data">
                    <div class="upload-area" onclick="document.getElementById('fileInput').click()">
                        <div>
                            <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" fill="currentColor" class="bi bi-cloud-upload text-primary mb-3" viewBox="0 0 16 16">
                                <path fill-rule="evenodd" d="M4.406 1.342A5.53 5.53 0 0 1 8 0c2.69 0 4.923 2 5.166 4.579C14.758 4.804 16 6.137 16 7.773 16 9.569 14.502 11 12.687 11H10a.5.5 0 0 1 0-1h2.688C13.979 10 15 8.988 15 7.773c0-1.216-1.02-2.228-2.313-2.228h-.5v-.5C12.188 2.825 10.328 1 8 1a4.53 4.53 0 0 0-2.941 1.1c-.757.652-1.153 1.438-1.153 2.055v.448l-.445.049C2.064 4.805 1 5.952 1 7.318 1 8.785 2.23 10 3.781 10H6a.5.5 0 0 1 0 1H3.781C1.708 11 0 9.366 0 7.318c0-1.763 1.266-3.223 2.942-3.593.143-.863.698-1.723 1.464-2.383z"/>
                                <path fill-rule="evenodd" d="M7.646 4.146a.5.5 0 0 1 .708 0l3 3a.5.5 0 0 1-.708.708L8.5 5.707V14.5a.5.5 0 0 1-1 0V5.707L5.354 7.854a.5.5 0 1 1-.708-.708l3-3z"/>
                            </svg>
                            <h4>Click to upload or drag and drop</h4>
                            <p class="text-muted">PDF files only (max 50MB)</p>
                        </div>
                        <input type="file" 
                               id="fileInput" 
                               name="file" 
                               accept=".pdf" 
                               required 
                               style="display: none;"
                               onchange="document.getElementById('fileName').textContent = this.files[0].name; document.getElementById('uploadBtn').disabled = false;">
                    </div>
                    
                    <div class="text-center mt-3">
                        <p id="fileName" class="text-muted">No file selected</p>
                        <button type="submit" 
                                id="uploadBtn" 
                                class="btn btn-primary btn-lg" 
                                disabled>
                            📤 Upload & Extract Text
                        </button>
                    </div>
                </form>

                <!-- Info Card -->
                <div class="card mt-5">
                    <div class="card-body">
                        <h5 class="card-title">🤖 How it works:</h5>
                        <ol>
                            <li>Upload your PDF document</li>
                            <li>System extracts text automatically</li>
                            <li>Choose a document to analyze with AI</li>
                            <li>Get AI-powered insights using Mistral (local & private)</li>
                        </ol>
                        <p class="text-muted mb-0">
                            <strong>100% Private:</strong> All processing happens locally on your Mac. 
                            No data sent to external servers!
                        </p>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="/webjars/jquery/3.7.1/jquery.min.js"></script>
    <script src="/webjars/bootstrap/5.3.0/js/bootstrap.bundle.min.js"></script>
</body>
</html>