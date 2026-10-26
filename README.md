# Eventify — Event Ticketing Android App

## 1. Project Overview
**Eventify** is an Android event-ticketing application designed for students and event organizers. Students can discover events and book tickets, while organizers can publish and manage events and review bookings.

The project is intended as an **intermediate-level academic project**, built with a straightforward structure and a professional, consistent user interface.

## 2. Objectives
- Provide separate Student and Organizer account experiences.
- Support registration, login, and logout using Firebase Authentication.
- Store user profiles, events, and booking information in Firebase.
- Allow organizers to create and manage events.
- Allow students to browse events and manage their tickets.
- Apply role-based access controls and basic input validation.
- Present a clean, responsive Android UI.

## 3. Technology Requirements

### Development
- **IDE:** Android Studio
- **Language:** Java
- **UI:** XML layouts
- **Build system:** Gradle
- **Minimum Android version:** To be selected based on the existing project configuration

### Firebase
- **Firebase Authentication:** Email/password registration, login, and logout
- **Cloud Firestore:** User profiles, organizer records, events, and bookings
- **Firebase Storage (optional):** Event poster/image uploads

### Optional Libraries / Enhancements
- **Material Components:** More polished Android UI components
- **ZXing or another QR-code library:** Digital ticket QR codes
- Image-loading library such as Glide: Event poster loading

## 4. User Roles

### 4.1 Student
A student should be able to:
- Register and sign in with an email address and password.
- Browse upcoming events.
- Search or filter events by name or category.
- View event details, venue, date, price, and ticket availability.
- Book one or more tickets when seats are available.
- View active tickets and booking history.
- View booking status and cancel a booking when cancellation is permitted.
- Log out.

### 4.2 Organizer
An organizer should be able to:
- Register or sign in with an email address and password.
- Provide a valid Organizer ID during registration.
- Create events with a title, description, date/time, venue, ticket price, and capacity.
- Edit or remove their own events when allowed.
- View events they created.
- Review bookings and attendee counts for their own events.
- Monitor remaining ticket availability.
- Log out.

## 5. Functional Requirements
1. The app must provide Student and Organizer account options.
2. Firebase Authentication must manage account credentials and sessions.
3. Each authenticated account must have a profile associated with its Firebase UID.
4. Each profile must store a role (`student` or `organizer`).
5. Organizer registration must validate an issued Organizer ID.
6. Login must route users to the dashboard matching their stored role.
7. The app must reject role mismatches and unauthenticated access to protected screens.
8. Organizers must only manage events they are authorized to manage.
9. Students must only access and manage their own booking records.
10. Booking must not exceed the available ticket capacity.
11. The app must validate required fields and display useful error messages.
12. Logout must sign the user out and return them to the login screen.
13. The app must display appropriate loading, empty, success, and failure states.

## 6. Non-Functional Requirements
- **Usability:** Simple navigation and clear labels.
- **Visual design:** Consistent colors, spacing, typography, and event cards.
- **Maintainability:** Separate activities, layouts, and model/helper classes where appropriate.
- **Reliability:** Handle network and Firebase errors without crashing.
- **Security:** Enforce access rules in Firebase, not only in Android UI code.
- **Data integrity:** Prevent duplicate or invalid booking operations and overbooking.
- **Performance:** Load event lists efficiently and avoid unnecessary database reads.
- **Compatibility:** Support the Android versions selected in the project configuration.

## 7. Proposed Firestore Data Structure

### `users/{uid}`
Suggested fields:
- `uid`
- `name`
- `email`
- `role`
- `organizerId` (empty or absent for students)
- `createdAt`

### `organizerCodes/{codeId}`
Suggested fields:
- `active`
- `usedByUid` (where applicable)
- Additional metadata such as `createdAt` or `createdBy`

### `events/{eventId}`
Suggested fields:
- `title`
- `description`
- `dateTime`
- `venue`
- `category`
- `price`
- `totalSeats`
- `availableSeats` (if maintained, updates must be transaction-safe)
- `organizerUid`
- `imageUrl` (optional)
- `createdAt`
- `status`

### `bookings/{bookingId}`
Suggested fields:
- `eventId`
- `studentUid`
- `organizerUid`
- `ticketCount`
- `totalAmount`
- `bookingStatus`
- `bookingTime`

Field names can be adjusted during implementation, but the app and Firestore documents must use the same names consistently.

## 8. Security Requirements
- Never store passwords in Firestore; Firebase Authentication manages credentials.
- Do not allow users to change their own role to `organizer`.
- Organizer IDs must be issued and validated through a trusted process.
- Do not rely on client-side checks alone for Organizer ID validation or single-use redemption.
- Configure Firestore Security Rules so users can read and update only permitted records.
- Restrict event creation and management to authorized organizers.
- Restrict booking access to the booking student and the relevant event organizer as appropriate.
- Use a Firestore transaction or trusted backend operation to enforce ticket limits.
- Do not deploy with open/test-mode Firestore rules.
- Do not include service-account credentials or private secrets in the Android app.

## 9. Main Screens
1. Login screen
2. Registration screen
3. Student dashboard / event listing
4. Event details screen
5. Booking confirmation screen
6. My Tickets / booking history screen
7. Organizer dashboard
8. Create/Edit Event screen
9. Organizer's Events screen
10. Event booking/attendee list
11. Logout and session handling

## 10. Suggested Development Phases
1. Verify Firebase Authentication and Firestore setup.
2. Build login, registration, role selection, and logout.
3. Store and retrieve user profiles.
4. Add secure Organizer ID validation.
5. Build event creation and event listing.
6. Add event details and ticket booking.
7. Add My Tickets and organizer booking management.
8. Add input validation, error handling, and UI polish.
9. Test role permissions, booking capacity, and Firebase Security Rules.
10. Optionally add event images and QR-code tickets.

## 11. Acceptance Criteria
The initial project is ready for demonstration when:
- Students can register, log in, and log out.
- Authorized organizers can register using a valid Organizer ID.
- Each role is routed to the correct dashboard.
- User profiles are saved in Firestore.
- Organizers can create events and students can view them.
- Students can book tickets without exceeding capacity.
- Students can view their own bookings and organizers can view bookings for their own events.
- Unauthorized access is blocked by Firebase Security Rules.
- Common failures show a useful message rather than crashing the app.

## 12. Out of Scope for the Initial Version
To keep the project manageable, these features are optional extensions:
- Online payment gateway
- Live seat-selection map
- Ticket resale or transfer
- Push notifications
- Advanced analytics
- QR-code scanning at the venue

## 13. Setup Notes
1. Open the project in Android Studio.
2. Keep the existing Firebase project and Android app registration.
3. Add Firebase Authentication and Cloud Firestore dependencies if they are not already present.
4. Enable Email/Password sign-in in Firebase Authentication.
5. Create the Firestore database and configure restrictive security rules.
6. Keep the Android package name consistent with the Firebase app registration.
7. Run the app and test Student and Organizer flows separately.

---
**Project name:** Eventify  
**Project type:** Android event ticketing application  
**Implementation:** Java, XML, Firebase Authentication, Cloud Firestore  
**Level:** Intermediate
