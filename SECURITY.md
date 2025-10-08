# Security Configuration Guide

## Environment Setup

### 1. API Keys
```bash
# Set environment variables (never commit these!)
export GEMINI_API_KEY="your_actual_gemini_api_key"
export DB_PASSWORD="your_postgres_password"
```

### 2. CORS Configuration
- Production: Update `WebConfig.java` with your actual domain
- Development: Currently allows `localhost:3000` and `localhost:8080`

### 3. File Upload Security
- Maximum file size: 10MB
- Allowed types: PDF, DOCX only
- Files are processed but not permanently stored

### 4. Database Security
- Use strong passwords
- Enable SSL in production
- Regular backups recommended

## Security Features Implemented

✅ **API Key Protection**: Moved to environment variables  
✅ **CORS Restriction**: Limited to specific origins  
✅ **Input Validation**: File size and type validation  
✅ **Error Handling**: No sensitive information in error messages  
✅ **Dependency Updates**: Updated vulnerable packages  
✅ **Logging Security**: Sanitized log messages  

## Production Checklist

- [ ] Set all environment variables
- [ ] Update CORS origins to production domains
- [ ] Enable HTTPS
- [ ] Set up proper logging infrastructure
- [ ] Configure database SSL
- [ ] Set up monitoring and alerting
- [ ] Regular security updates

## Reporting Security Issues

If you find a security vulnerability, please email: security@yourcompany.com