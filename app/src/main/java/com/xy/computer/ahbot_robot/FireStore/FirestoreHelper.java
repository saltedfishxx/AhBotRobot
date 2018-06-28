
package com.xy.computer.ahbot_robot.FireStore;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.xy.computer.ahbot_robot.MainActivity;
import com.xy.computer.ahbot_robot.Medicine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.ContentValues.TAG;

/**
 * Created by user on 21/3/2018.
 */

public class FirestoreHelper {
    List<Medicine> medicineList;

    static CollectionReference medicineCollection = FirebaseFirestore.getInstance().collection("MedicineList");

    public FirestoreHelper(MainActivity r) {


        final MainActivity reference = r;
        medicineCollection
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("FirestoreHelper", "Listen failed.", e);
                            return;
                        }
                        List<Medicine> allmedicineList = new ArrayList<>();

                        //medicineList.clear();
                        for (DocumentSnapshot document : value) {

                            String id = (String) document.get("Id");
                            String medName = (String) document.get("MedicineName");
                            String amt = (String) document.get("Amount");
                            String freq = (String) document.get("Frequency");
                            String remarks = (String) document.get("Remarks");


                            Medicine medicine = new Medicine(id, medName, amt, freq, remarks);
                            allmedicineList.add(medicine);
                        }
                        reference.getMedicine(allmedicineList);
                    }
                });

    }

    public FirestoreHelper() {
    }

    public void storeMedicine(MainActivity r) {
        final MainActivity ref = r;

        medicineCollection.get().
                addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull com.google.android.gms.tasks.Task<QuerySnapshot> task) {

                        medicineList = new ArrayList<>();
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                String id = (String) document.get("Id");
                                String medName = (String) document.get("MedicineName");
                                String amt = (String) document.get("Amount");
                                String freq = (String) document.get("Frequency");
                                String remarks = (String) document.get("Remarks");


                                Medicine medicine = new Medicine(id, medName, amt, freq, remarks);
                                medicineList.add(medicine);

                            }
                            ref.getMedicine(medicineList);

                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                            return;
                        }
                    }
                });
    }


    public static void deleteData(Medicine med) {
        Medicine medicine = med;
        String id = medicine.getId();
        medicineCollection.document(id)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully deleted!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error deleting document", e);
                    }
                });
    }


    //one method to add one 'row' of data
    public static void saveData(Medicine medicine) {
        Map<String, String> data = new HashMap<String, String>();
        data.put("MedicineName", medicine.getMedName());
        data.put("Amount", medicine.getMedAmount());
        data.put("Frequency", medicine.getMedFrequency());
        data.put("Remarks", medicine.getRemarks());
        medicineCollection.document().set(data).addOnSuccessListener(new OnSuccessListener<Void>() {
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

    public static void updateData(Medicine med) {
        medicineCollection.document(med.getId())
                .update("MedicineName", med.getMedName(),
                        "Amount", med.getMedAmount(),
                        "Frequency", med.getMedFrequency(),
                        "Remarks", med.getRemarks())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully updated!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error updating document", e);
                    }
                });
    }

    //method to add all data
    public void saveAllData(List<Medicine> medList) {
        for (Medicine medicine : medList) {
            saveData(medicine);
        }
    }

}