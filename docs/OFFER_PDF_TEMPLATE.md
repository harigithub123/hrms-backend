# Offer letter PDF template

Generated offers can use **your existing PDF letterhead** (same look as `HariNale_Offer_Letter_*.pdf`).

## Option A — Bundled template (recommended)

1. Copy your PDF to:
   - `hrms-backend/src/main/resources/templates/offer-letter-template.pdf`
2. Rebuild the backend. The app loads this file from the classpath and **draws the merged offer text on top of page 1** (Helvetica, body area).

## Option B — External file path

Set an environment variable or `application.yml`:

```yaml
hrms:
  offer:
    pdf-template-file: "C:/path/to/HariNale_Offer_Letter_20220115_133603.pdf"
```

Or:

```bash
set HRMS_OFFER_PDF_TEMPLATE_FILE=C:\path\to\your\offer-template.pdf
```

## Tuning layout

If text overlaps the logo or sits too low, adjust (PDF points, bottom-left origin; A4 height ≈ 842):

```yaml
hrms:
  offer:
    body-start-y: 620    # higher value = text starts higher on the page
    body-margin-x: 50
    body-min-y: 72       # when below this, a new page is added
    body-line-height: 12
    wrap-chars: 85
```

## Fallback

If neither the classpath file nor `pdf-template-file` exists, the app generates a **plain one-page PDF** (previous behaviour).
