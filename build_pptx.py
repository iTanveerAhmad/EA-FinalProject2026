#!/usr/bin/env python3
"""
Creates ReleaseManagementSystem_Presentation.pptx using ONLY Python standard library.
No pip install required.
"""
import zipfile
import os
import tempfile
import shutil

# Slide content
SLIDES = [
    ("Real-Time Release Management System", "CS544 Enterprise Architecture - Project Presentation", True),
    ("Project Overview", [
        "Comprehensive platform for managing software releases",
        "Full release lifecycle: task assignment to hotfixes",
        "Event-driven architecture (Kafka)",
        "Real-time collaboration (SSE)",
        "AI assistance (Ollama)",
        "Email notifications",
        "Prometheus + Grafana monitoring"
    ], False),
    ("System Architecture", [
        "Release Service (8080): Core logic, Kafka, MongoDB",
        "Notification Service (8081): Kafka consumer, Gmail SMTP",
        "Frontend (5173): React, TypeScript, Vite",
        "Infrastructure: Kafka, MongoDB, Ollama, Prometheus, Grafana"
    ], False),
    ("Workflow Enforcement", [
        "Single In-Process: One task per developer",
        "Sequential: Complete tasks in order",
        "Hotfix: Auto re-open completed releases"
    ], False),
    ("Kafka Events", [
        "task-events: assigned, hotfix, stale, completed",
        "system-events: error",
        "Release Service produces, Notification Service consumes"
    ], False),
    ("Authentication", [
        "JWT-based: register, login, /auth/me",
        "Roles: ADMIN, DEVELOPER",
        "Email stored for task notifications",
        "bcrypt password encryption"
    ], False),
    ("Real-Time Features", [
        "Activity Feed: SSE at /activity/stream",
        "AI Chat: Ollama integration",
        "Forum: Nested comments on tasks"
    ], False),
    ("Notification Flow", [
        "Admin assigns task -> ReleaseService",
        "Lookup developer email -> Publish to Kafka",
        "Notification Service consumes -> Send email",
        "Log to MongoDB"
    ], False),
    ("API Endpoints", [
        "Auth, Releases, Tasks, Comments",
        "Activity stream (SSE), Chat",
        "Test: GET /test/email?to=..."
    ], False),
    ("Technology Stack", [
        "Java 21, Spring Boot, MongoDB, Kafka",
        "React, TypeScript, Vite",
        "Ollama, Gmail SMTP, Docker"
    ], False),
    ("How to Run", [
        "mvn clean package -DskipTests",
        "docker-compose up -d",
        "Frontend: localhost:5173",
        "APIs: 8080, 8081"
    ], False),
    ("Thank You", "Questions?", True),
]

# Minimal valid PPTX - base64 encoded single-slide template (simplified structure)
# We'll create the zip structure manually
def escape_xml(text):
    return (str(text)
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
        .replace("'", "&apos;"))

def make_slide_xml(title, bullets, is_title_slide):
    # Slide root must be p:sld, not p:cSld. Shapes need xfrm for position/size.
    # spPr with a:xfrm and a:off, a:ext for proper placement
    xfrm = '''<p:spPr><a:xfrm><a:off x="914400" y="914400"/><a:ext cx="7315200" cy="914400"/></a:xfrm><a:prstGeom prst="rect"/></p:spPr>'''
    body_pr = '<a:bodyPr wrap="square" rtlCol="0"><a:spAutoFit/></a:bodyPr>'
    
    if is_title_slide:
        subtitle = bullets if isinstance(bullets, str) else ""
        content = f'''        <a:p><a:r><a:rPr lang="en-US" sz="4400"/><a:t>{escape_xml(title)}</a:t></a:r><a:endParaRPr lang="en-US"/></a:p>
        <a:p><a:r><a:rPr lang="en-US" sz="3200"/><a:t>{escape_xml(subtitle)}</a:t></a:r><a:endParaRPr lang="en-US"/></a:p>'''
    else:
        lines = [f'        <a:p><a:r><a:rPr lang="en-US" sz="3600"/><a:t>{escape_xml(title)}</a:t></a:r><a:endParaRPr lang="en-US"/></a:p>']
        for b in bullets:
            lines.append(f'        <a:p><a:r><a:rPr lang="en-US" sz="2800"/><a:t>{escape_xml(b)}</a:t></a:r><a:endParaRPr lang="en-US"/></a:p>')
        content = '\n'.join(lines)
    
    return f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <p:cSld>
    <p:bg><p:bgPr><a:solidFill><a:srgbClr val="FFFFFF"/></a:solidFill></p:bgPr></p:bg>
    <p:spTree>
      <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/></p:nvGrpSpPr>
      <p:grpSpPr/>
      <p:sp>
        <p:nvSpPr><p:cNvPr id="2" name="Title 1"/><p:cNvSpPr txBox="1"/></p:nvSpPr>
        {xfrm}
        <p:txBody>
          {body_pr}
          <a:lstStyle/>
{content}
        </p:txBody>
      </p:sp>
    </p:spTree>
  </p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sld>'''

def main():
    import time
    base = os.path.dirname(os.path.abspath(__file__)) or "."
    tmpdir = os.path.join(base, f"_pptx_build_{int(time.time())}")
    os.makedirs(tmpdir, exist_ok=True)
    try:
        # Create directory structure
        os.makedirs(f"{tmpdir}/_rels", exist_ok=True)
        os.makedirs(f"{tmpdir}/docProps", exist_ok=True)
        os.makedirs(f"{tmpdir}/ppt/_rels", exist_ok=True)
        os.makedirs(f"{tmpdir}/ppt/slides/_rels", exist_ok=True)
        os.makedirs(f"{tmpdir}/ppt/slideLayouts/_rels", exist_ok=True)
        os.makedirs(f"{tmpdir}/ppt/slideMasters/_rels", exist_ok=True)
        os.makedirs(f"{tmpdir}/ppt/theme/_rels", exist_ok=True)

        # [Content_Types].xml
        ct_parts = []
        for i in range(1, len(SLIDES) + 1):
            ct_parts.append(f'  <Override PartName="/ppt/slides/slide{i}.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>')
        content_types = f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
{chr(10).join(ct_parts)}
  <Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/>
  <Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>
  <Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>
  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
</Types>'''
        with open(f"{tmpdir}/[Content_Types].xml", "w", encoding="utf-8") as f:
            f.write(content_types)

        # _rels/.rels
        with open(f"{tmpdir}/_rels/.rels", "w", encoding="utf-8") as f:
            f.write('''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>''')

        # docProps
        with open(f"{tmpdir}/docProps/core.xml", "w", encoding="utf-8") as f:
            f.write('''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties"><dc:title xmlns:dc="http://purl.org/dc/elements/1.1/">Release Management System</dc:title><dc:creator xmlns:dc="http://purl.org/dc/elements/1.1/">CS544</dc:creator><cp:lastModifiedBy>CS544</cp:lastModifiedBy></cp:coreProperties>''')
        with open(f"{tmpdir}/docProps/app.xml", "w", encoding="utf-8") as f:
            f.write('''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"><Application>Python</Application><Slides>13</Slides></Properties>''')

        # ppt/_rels/presentation.xml.rels
        rels = ['  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="slideMasters/slideMaster1.xml"/>']
        for i in range(1, len(SLIDES) + 1):
            rels.append(f'  <Relationship Id="rId{i+1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide{i}.xml"/>')
        with open(f"{tmpdir}/ppt/_rels/presentation.xml.rels", "w", encoding="utf-8") as f:
            f.write('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">\n' + '\n'.join(rels) + '\n</Relationships>')

        # ppt/presentation.xml
        sldId_parts = "".join(f'<p:sldId id="{256+i}" r:id="rId{i+1}"/>' for i in range(len(SLIDES)))
        with open(f"{tmpdir}/ppt/presentation.xml", "w", encoding="utf-8") as f:
            f.write(f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <p:sldMasterIdLst><p:sldMasterId id="2147483648" r:id="rId1"/></p:sldMasterIdLst>
  <p:sldIdLst>{sldId_parts}</p:sldIdLst>
  <p:sldSz cx="9144000" cy="6858000"/>
  <p:notesSz cx="6858000" cy="9144000"/>
</p:presentation>''')

        # ppt/theme/theme1.xml (minimal)
        with open(f"{tmpdir}/ppt/theme/theme1.xml", "w", encoding="utf-8") as f:
            f.write('''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="Office Theme">
  <a:themeElements><a:clrScheme name="Office"><a:dk1><a:sysClr val="windowText" lastClr="000000"/></a:dk1><a:lt1><a:sysClr val="window" lastClr="FFFFFF"/></a:lt1></a:clrScheme></a:themeElements>
</a:theme>''')

        # ppt/theme/_rels/theme1.xml.rels
        with open(f"{tmpdir}/ppt/theme/_rels/theme1.xml.rels", "w", encoding="utf-8") as f:
            f.write('<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"/>')

        # ppt/slideMasters/slideMaster1.xml (minimal)
        with open(f"{tmpdir}/ppt/slideMasters/slideMaster1.xml", "w", encoding="utf-8") as f:
            f.write('''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldMaster xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
  <p:cSld><p:bg><p:bgPr><a:solidFill><a:srgbClr val="FFFFFF"/></a:solidFill></p:bgPr></p:bg><p:spTree/></p:cSld>
  <p:clrMap bg1="lt1" tx1="dk1" bg2="lt2" tx2="dk2"/>
</p:sldMaster>''')

        # ppt/slideMasters/_rels/slideMaster1.xml.rels
        with open(f"{tmpdir}/ppt/slideMasters/_rels/slideMaster1.xml.rels", "w", encoding="utf-8") as f:
            f.write('<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="../theme/theme1.xml"/></Relationships>')

        # ppt/slideLayouts/slideLayout1.xml (minimal)
        with open(f"{tmpdir}/ppt/slideLayouts/slideLayout1.xml", "w", encoding="utf-8") as f:
            f.write('''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldLayout xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
  <p:cSld><p:spTree/></p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sldLayout>''')

        # ppt/slideLayouts/_rels/slideLayout1.xml.rels
        with open(f"{tmpdir}/ppt/slideLayouts/_rels/slideLayout1.xml.rels", "w", encoding="utf-8") as f:
            f.write('<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="../slideMasters/slideMaster1.xml"/></Relationships>')

        # Create slides
        for i, (title, content, is_title) in enumerate(SLIDES):
            slide_num = i + 1
            if is_title:
                bullets = content if isinstance(content, str) else ""
            else:
                bullets = content if isinstance(content, list) else []
            slide_xml = make_slide_xml(title, bullets, is_title)
            with open(f"{tmpdir}/ppt/slides/slide{slide_num}.xml", "w", encoding="utf-8") as f:
                f.write(slide_xml)
            with open(f"{tmpdir}/ppt/slides/_rels/slide{slide_num}.xml.rels", "w", encoding="utf-8") as f:
                f.write(f'<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/></Relationships>')

        # Create zip (pptx)
        output = os.path.join(base, "ReleaseManagementSystem_Presentation.pptx")
        with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as zf:
            for root, dirs, files in os.walk(tmpdir):
                for f in files:
                    path = os.path.join(root, f)
                    arcname = path[len(tmpdir)+1:].replace("\\", "/")
                    zf.write(path, arcname)

        print(f"Created: {output}")
        print(f"Slides: {len(SLIDES)}")
    finally:
        if os.path.exists(tmpdir):
            try:
                shutil.rmtree(tmpdir, ignore_errors=True)
            except Exception:
                pass

if __name__ == "__main__":
    main()
