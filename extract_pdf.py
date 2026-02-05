#!/usr/bin/env python
import sys

try:
    import pdfplumber
    with pdfplumber.open('EA-ProjectJan2026.pdf') as pdf:
        for i, page in enumerate(pdf.pages):
            print(f'\n--- PAGE {i+1} ---\n')
            text = page.extract_text()
            if text:
                print(text)
except ImportError:
    print('pdfplumber not available, trying pypdf')
    try:
        import PyPDF2
        with open('EA-ProjectJan2026.pdf', 'rb') as f:
            reader = PyPDF2.PdfReader(f)
            for i, page in enumerate(reader.pages):
                print(f'\n--- PAGE {i+1} ---\n')
                text = page.extract_text()
                if text:
                    print(text)
    except ImportError:
        print('PyPDF2 not available. Please install: pip install pdfplumber')
except Exception as e:
    print(f'Error: {e}')
