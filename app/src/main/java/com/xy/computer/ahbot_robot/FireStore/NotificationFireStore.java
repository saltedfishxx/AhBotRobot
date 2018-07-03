package com.xy.computer.ahbot_robot.FireStore;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.xy.computer.ahbot_robot.MainActivity;
import com.xy.computer.ahbot_robot.Medicine;

import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;

public class NotificationFireStore {
    public NotificationFireStore(MainActivity r){
        MainActivity ref = r;
    }

    static CollectionReference textToSend = FirebaseFirestore.getInstance().collection("NotificationList");
    public static void saveData(String s) {
        Map<String, String> data = new HashMap<String, String>();
        data.put("Notification", s);
        textToSend.document().set(data).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d("FirestoreHelper", "Document has been saved!");
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });

    }
}
