# Admin Reclamations Management - Implementation Guide

## Overview
A complete admin interface for managing user reclamations with search and filter capabilities, matching the existing admin dashboard styling.

## Features Implemented

### 1. **Search & Filter Functionality**
- **Search by**: Content, user name, or email
- **Filter by Status**:
  - Tous les statuts (All)
  - En attente (Pending)
  - En cours (In Progress)
  - Répondu (Answered)
  - Résolu (Resolved)
  - Rejeté (Rejected)
- **Filter by Type**:
  - Tous les types (All)
  - Compte (Account)
  - Bug
  - Coaching
  - Paiement (Payment)
  - Autre (Other)

### 2. **Reclamation List Display**
Each reclamation row shows:
- **Reclamation ID** (e.g., "Réclamation #123")
- **User Information** (User ID)
- **Content Preview** (truncated to 120 characters)
- **Creation Date** (formatted as "dd/MM/yyyy à HH:mm")
- **Status Badge** (color-coded)
- **Type Badge**
- **Action Buttons**:
  - 👁 **Voir** (View) - Shows full details with all responses
  - ✉ **Répondre** (Reply) - Opens reply dialog

### 3. **View Details Dialog**
Shows complete information:
- Reclamation ID, Type, Status
- User ID
- Creation date
- Full content (HTML stripped)
- All responses with timestamps

### 4. **Reply Functionality**
- Opens a dialog to compose a response
- Minimum 5 characters required
- Automatically changes status to "Répondu" (Answered)
- Saves response to database
- Refreshes the list after submission

### 5. **Total Count Display**
Shows the number of reclamations matching current filters

## Files Created/Modified

### New Files:
1. **`DayFlow/src/main/java/controllers/admin/AdminReclamationsController.java`**
   - Main controller with search/filter logic
   - View and reply functionality
   - Pagination support (50 items per page)

2. **`DayFlow/src/main/resources/admin/admin_reclamations.fxml`**
   - FXML layout matching admin dashboard style
   - Filter controls and reclamation list

### Modified Files:
1. **`DayFlow/src/main/java/controllers/admin/AdminShellController.java`**
   - Added `navReclamationsBtn` field
   - Added `onNavReclamations()` method
   - Added `loadReclamations()` method
   - Updated `mainNavButtons` list

2. **`DayFlow/src/main/resources/admin/admin_shell.fxml`**
   - Added "Réclamations" navigation button with proper styling

## Styling
The interface uses the existing admin.css styles:
- **`.admin-root`** - Main background (pastel purple)
- **`.admin-content`** - Content padding and spacing
- **`.admin-page-title`** - Page title styling
- **`.admin-card`** - White card containers
- **`.admin-filter-field`** - Input fields
- **`.admin-filter-btn`** - Purple action buttons
- **`.admin-list-row`** - List item rows with borders
- **`.admin-badge`** - Status/type badges with color coding

### Badge Colors:
- **Accepted/Resolved**: Green (`badge-accepted`)
- **Pending**: Yellow (`badge-pending`)
- **In Progress**: Light yellow (`badge-scheduling`)
- **Default**: Gray (`badge-default`)

## Usage

### Accessing the Interface:
1. Log in as an admin user
2. Navigate to the admin dashboard
3. Click on **"Réclamations"** in the left sidebar

### Searching & Filtering:
1. Enter search terms in the search field (searches content, user name, email)
2. Select a status from the dropdown (optional)
3. Select a type from the dropdown (optional)
4. Click **"🔍 Filtrer"** to apply filters
5. Click **"✖ Effacer"** to clear all filters

### Viewing Details:
1. Click the **"👁 Voir"** button on any reclamation
2. A dialog will show complete information including all responses

### Replying to Reclamations:
1. Click the **"✉ Répondre"** button on any reclamation
2. Enter your response (minimum 5 characters)
3. Click **OK** to send
4. The status will automatically change to "Répondu"
5. The list will refresh to show the updated status

## Database Integration
Uses existing services:
- **`ReclamationService`** - For querying and updating reclamations
  - `findForAdmin()` - Search with filters
  - `countForAdmin()` - Count matching reclamations
  - `findByIdWithResponses()` - Get full details
  - `addAdminReply()` - Add response and update status

## Technical Details

### Pagination:
- Currently set to 50 items per page (`PAGE_SIZE = 50`)
- Can be extended to support multiple pages if needed

### HTML Content Handling:
- Strips HTML tags from content for display
- Handles common HTML entities (&nbsp;, &amp;, etc.)
- Preserves line breaks from `<br>` and `<p>` tags

### Date Formatting:
- Uses French locale: "dd/MM/yyyy 'à' HH:mm"
- Example: "26/04/2026 à 19:45"

### Error Handling:
- SQL exceptions show error alerts
- Validation errors show warning alerts
- Success confirmations after actions

## Future Enhancements (Optional)
- Add pagination controls for large result sets
- Export reclamations to CSV/Excel
- Bulk status updates
- Email notifications when replying
- Attachment viewing (photos)
- Advanced date range filters
- User profile quick view
- Response templates
- Statistics dashboard

## Testing Checklist
- [ ] Navigation to reclamations page works
- [ ] Search by content works
- [ ] Search by user name/email works
- [ ] Status filter works
- [ ] Type filter works
- [ ] Combined filters work
- [ ] Clear filters resets all fields
- [ ] View details shows complete information
- [ ] Reply dialog opens and validates input
- [ ] Reply saves and updates status
- [ ] List refreshes after reply
- [ ] Total count updates correctly
- [ ] Styling matches other admin pages
- [ ] Badges display correct colors
- [ ] Date formatting is correct
- [ ] HTML content is properly stripped
