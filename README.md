### Echo: Another Social Media App

#### Development

To connect firebase add the `google-services.json` in app directory.

#### Firebase Database Rules

```
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if false;
    }
    match /users/{userId} {
      allow read, write: if true;
    }
    match /posts/{postId} {
      allow read, write: if request.auth != null;
      allow update: if request.auth != null && request.resource.data.likes is list;
    }
   match /posts/{postId}/comments/{commentId} {
  		allow read, write: if request.auth != null;
		}
    match /users/{userId}/posts/{postId} {
      allow read: if true
    }
    match /posts/{postId} {
  		allow delete: if request.auth != null && resource.data.userId == request.auth.uid;
 		}
  }
}
```