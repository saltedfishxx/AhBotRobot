const functions = require('firebase-functions');

const firebase = require('firebase-admin');

var registrationToken = "edmRFYXvPYU:APA91bFbpX-afHjUJJB18d_5DVcu-gSxZzK5Cwnnf90dca0_1rUvlmmMFRNYzDoctOlWqP5bVc9iZmlAPzIRNZjxnA8ythSzRvPf3J4BPv469NWe4iGdFpmBUh6q3eJnzWVuCu51Aw2Kw8_mYF4BcBr9HRY_jcuFOA"


firebase.initializeApp(functions.config().firestore);
exports.sendFoodNotification = functions.firestore.document("NotificationList/{foodrecipes}").onCreate((snap, context) => {
  console.log('notification triggerd');
  var payload = {
    notification: {
      title: "Notification",
      body: "Check your recipe now!"
  }

  };

  return firebase.messaging().sendToDevice(registrationToken, payload)
    .then((response) => {
      console.log("Successfully sent message:", response);
      return true;
    })
    .catch((error) => {
      console.log("Error sending message:", error);
    });
  console.log("Sent notification.")

});


exports.webhook = functions.https.onRequest((request, response) => {



  switch (request.body.result.action) { //get the specific action

      //ACTION 1 
      case 'storeProfile':

          console.log("request.body.result.parameters: ", request.body.result.parameters); //get parameters from dialogflow intent
          let params = request.body.result.parameters;

          // var user = firebase.auth().currentUser;

          //make strings in lists unique
          var diseases = (params.diseases).filter(function (value, index) { return (params.diseases).indexOf(value) == index });
          var allergies = (params.allergies).filter(function (value, index) { return (params.allergies).indexOf(value) == index });

          var speechProfile = "OK. Thanks " + params.name + ". Your profile has been created."


          firestore.collection('profiles') //get profiles from firestore
              .add({
                  name: params.name,
                  age: params.age,
                  diseases: diseases,
                  allergies: allergies
              })
              .then(() => {
                  response.send({ speech: speechProfile })

              }).catch((e => {
                  console.log("error: went wrong : ", e);
                  response.send({ speech: "something went wrong when writing to the database" })
              }))

          break;

      case 'getDisease':


          firestore.collection('profiles').get() //get profiles from firestore
              .then(function (querySnapshot) {
                  var profiles = [];
                  querySnapshot.forEach(function (doc) {
                      profiles.push(doc.data())
                  });

                  var count = 0;
                  profiles.forEach((profile, index) => { count += 1; }) //get profile count


                  if (count > 1) { //if there is more than one profile
                      var profileNames = "";

                      var profileCount = 0;
                      profiles.forEach((profile, index) => {
                          profileCount += 1;
                          profileNames += profile.name + ". or ";
                      })
                      profileNames = profileNames.slice(0, -4); //remove ' or ' at end

                      profileSpeech = "You have " + profileCount + " profiles. To get information on your disease. Please choose a profile from either. " + profileNames;

                      response.send({ speech: profileSpeech })


                  } else if (count == 1) { //if there is only one profile

                      //extract diseases from the profile
                      var diseaseCount = 0;
                      var diseases = [];
                      profiles.forEach((profile, index) => {
                          diseases = profile.diseases;
                      })

                      diseases.forEach((disease, index) => { diseaseCount += 1; }) //get disease count

                      if (diseaseCount == 1) { //if profile has one disease


                          diseases.forEach((disease, index) => { //foreach disease in diseases (only 1 disease)


                              if (disease.toLowerCase() == 'no diseases') { //check if the user has no diseases (nil)

                                  response.send({ speech: "You have no diseases" })

                              } else {

                                  //get disease information from firestore
                                  var diseaseInformation = "hi";
                                  firestore.collection("diseases")
                                      .get()
                                      .then(function (querySnapshot) {
                                          var diseases = [];

                                          querySnapshot.forEach(function (doc) {

                                              diseases.push(doc.data())
                                          });

                                          diseases.forEach((ds, index) => {
                                              if ((ds.disease).toString() == disease.toString()) {
                                                  diseaseInformation = ds.diseaseInformation;
                                              }
                                          })

                                          var speechDisease = "You have one disease, which is " + disease + ". " + diseaseInformation;

                                          if (diseaseInformation == "hi") {
                                              response.send({ speech: "There is no information on the disease, " + disease + ", yet." })
                                          } else {
                                              response.send({ speech: speechDisease })
                                          }

                                      }) //end then

                              } //end if-else

                          }) //end foreach



                      } else {  //if disease count is > 1

                          //give user choice of diseases
                          var speechDiseases = "Pick one of your diseases to learn more about. Your diseases are ";
                          diseases.forEach((disease, index) => { speechDiseases += disease + " or "; })

                          speechDiseases = speechDiseases.slice(0, -4);

                          response.send({ speech: speechDiseases })
                      }


                  } else {  //if profile count is 0   
                      response.send({ speech: "You have no profiles yet. Create one by saying Create medical profile" })
                  }

              }).catch(err => {
                  console.log("error: went wrong : ", err);
                  response.send({
                      speech: "An error has occurred " + err
                  })
              });


          break;

      //ACTION 2
      case 'getDiseasesFromProfileName':


          console.log("request.body.result.parameters: ", request.body.result.parameters); //get parameters from dialogflow intent
          let paramsName = request.body.result.parameters;


          firestore.collection('profiles')
              .get() //get profiles from firestore
              .then(function (querySnapshot) {
                  var profiles = [];
                  querySnapshot.forEach(function (doc) {
                      profiles.push(doc.data())
                  });

                  //extract diseases from the specified profile
                  var diseaseCount = 0;
                  var diseases = [];
                  profiles.forEach((profile, index) => {
                      if ((profile.name).toString() == (paramsName.name).toString()) { diseases = profile.diseases; }
                  })

                  diseases.forEach((disease, index) => { diseaseCount += 1; }) //get disease count

                  if (diseaseCount == 1) { //if profile has one disease


                      diseases.forEach((disease, index) => { //foreach disease in diseases (only 1 disease)


                          if (disease.toLowerCase() == 'no diseases') { //check if the user has no diseases (nil)

                              response.send({ speech: "You have no diseases" })

                          } else {

                              //get disease information from firestore
                              var diseaseInformation = "hi";
                              firestore.collection("diseases")
                                  .get()
                                  .then(function (querySnapshot) {
                                      var diseases = [];

                                      querySnapshot.forEach(function (doc) {

                                          diseases.push(doc.data())
                                      });

                                      diseases.forEach((ds, index) => {
                                          if ((ds.disease).toString() == disease.toString()) {
                                              diseaseInformation = ds.diseaseInformation;

                                          }
                                      })
                                      var speechDisease1 = "Your disease is " + disease + ". Sadly, There is no information on that disease in our database, yet";
                                      var speechDisease2 = "You have one disease, " + disease + ". " + diseaseInformation;

                                      if (diseaseInformation == "hi") {
                                          response.send({ speech: speechDisease1 })
                                      } else {
                                          response.send({ speech: speechDisease2 })
                                      }

                                  }) //end then

                          } //end if-else

                      }) //end foreach



                  } else {  //if disease count is > 1

                      //give user choice of diseases
                      var speechDiseases = "Pick one of your diseases to learn more about. Your diseases are ";
                      diseases.forEach((disease, index) => { speechDiseases += disease + " or "; })

                      speechDiseases = speechDiseases.slice(0, -4);

                      response.send({ speech: speechDiseases })
                  }


              })

          break;


      //ACTION 3
      case 'getDiseaseInfo':
          console.log("request.body.result.parameters: ", request.body.result.parameters);

          let paramsDisease = request.body.result.parameters;

          var diseaseInformation = "hi";
          firestore.collection("diseases")
              .get()
              .then(function (querySnapshot) {
                  var diseases = [];

                  querySnapshot.forEach(function (doc) {

                      diseases.push(doc.data())
                  });


                  diseases.forEach((ds, index) => {
                      if ((ds.disease).toString() == (paramsDisease.disease).toString()) { diseaseInformation = ds.diseaseInformation; }
                  })


                  var speechDisease = "Disease information is as follows. " + diseaseInformation;

                  if (diseaseInformation == "hi") {

                      response.send({ speech: "There is no information on that disease, yet" })

                  } else {

                      response.send({ speech: speechDisease })
                  }
              })

          break;

      //ACTION 4
      case 'checkNameOnCreateProfile':

          console.log("request.body.result.parameters: ", request.body.result.parameters);
          let paramsCheckName = request.body.result.parameters;


          firestore.collection('profiles') //get profiles from firestore
              .get() //get profiles from firestore
              .then(function (querySnapshot) {
                  var profiles = [];
                  querySnapshot.forEach(function (doc) {
                      profiles.push(doc.data())
                  });

                  //extract diseases from the specified profile
                  var nameCount = 0;

                  profiles.forEach((profile, index) => {
                      if ((profile.name).toString() == (paramsCheckName.user_name).toString()) { nameCount += 1; }
                  })

                  var speechConfirm = "Got it, so your name is " + paramsCheckName.user_name + ", age is " + paramsCheckName.user_age + ", you have " + paramsCheckName.user_diseases + ", and you're allergic to " + paramsCheckName.user_allergies + "? Is that correct?";

                  if (nameCount > 0) {
                      response.send({ speech: "Your profile can't be created. There is already another profile with that name, please say another name." })
                  } else {
                      response.send({ speech: speechConfirm })
                  }

              })

          break;


      //ACTION 5
      case 'checkProfileToDelete':
          console.log("request.body.result.parameters: ", request.body.result.parameters);
          let paramsCheckProfileToDelete = request.body.result.parameters;

          firestore.collection('profiles') //get profiles from firestore
              .get() //get profiles from firestore
              .then(function (querySnapshot) {
                  var profiles = [];
                  querySnapshot.forEach(function (doc) {
                      profiles.push(doc.data())
                  });
                  var profileNames = "";

                  var profileCount = 0;
                  profiles.forEach((profile, index) => {
                      profileCount += 1;
                      profileNames += profile.name + ". Or ";
                  })
                  profileNames = profileNames.slice(0, -4); //remove ' or ' at end

                  if (profileCount == 0) {
                      response.send({ speech: "You have no profiles to delete. Say create profile if you want to make a new medical profile." })
                  } else {
                      var profileSpeech = "You have " + profileCount + " profiles. Please choose a profile to delete. " + profileNames;

                      response.send({ speech: profileSpeech })
                  }



              })

          break;


      //ACTION 6
      case 'deleteProfile-confirm-yes':

          //confirm deletion

          console.log("request.body.result.parameters: ", request.body.result.parameters);
          let paramsDeleteName = request.body.result.parameters;


          firestore.collection('profiles')
              .get() //get profiles from firestore
              .then(function (querySnapshot) {
                  var profiles = [];
                  querySnapshot.forEach(function (doc) {
                      profiles.push(doc.data())
                  });


                  var nameCount = 0;

                  //delete profile that matches names with parameter name
                  profiles.forEach((profile, index) => {
                      if ((profile.name).toString() == (paramsDeleteName.name).toString()) {

                          nameCount += 1;

                          var deleteProfile_query = firestore.collection('profiles').where('name', '==', paramsDeleteName.name.toString());

                          deleteProfile_query
                              .get()
                              .then(function (querySnapshot) {
                                  querySnapshot.forEach(function (doc) {
                                      doc.ref.delete();
                                  });
                              }).catch((e => {
                                  console.log("error: went wrong : ", e);
                                  response.send({ speech: "Something went wrong: " + e })
                              }));
                      }

                  })

                  if (nameCount > 0) {
                      response.send({ speech: "The profile of " + paramsDeleteName.name + ". has been deleted successfully." })
                  } else {
                      response.send({ speech: "The profile cannot be deleted as it can't be found in the database." })
                  }


              }).catch((e => {
                  console.log("error: went wrong : ", e);
                  response.send({ speech: "Something went wrong: " + e })
              }));

          break;



      //ACTION 7 check profile name to add data to
      case 'checkNamesOfProfiles':
          console.log("request.body.result.parameters: ", request.body.result.parameters);
          let paramsCheckNamesOfProfiles = request.body.result.parameters;

          firestore.collection('profiles')
              .get() //get profiles from firestore
              .then(function (querySnapshot) {
                  var profiles = [];
                  querySnapshot.forEach(function (doc) {
                      profiles.push(doc.data())
                  });
                  var profileNames = "";

                  var profileCount = 0;
                  profiles.forEach((profile, index) => {
                      profileCount += 1;
                      profileNames += profile.name + ". Or ";
                  })
                  profileNames = profileNames.slice(0, -4); //remove ' or ' at end

                  if (profileCount == 0) {
                      response.send({ speech: "You have no profiles to delete. Say create profile if you want to make a new medical profile." })

                  } else {
                      var profileSpeech = "You have " + profileCount + " profiles. Please choose a profile to add data to. from. " + profileNames;

                      response.send({ speech: profileSpeech })
                  }



              }).catch((e => {
                  console.log("error: went wrong : ", e);
                  response.send({ speech: "Something went wrong: " + e })
              }));


          break;

      case 'checkNames':

          console.log("request.body.result.parameters: ", request.body.result.parameters);
          let paramsNameExists1 = request.body.result.parameters;
          firestore.collection('profiles')
              .get() //get profiles from firestore
              .then(function (querySnapshot) {
                  var profiles = [];
                  querySnapshot.forEach(function (doc) {
                      profiles.push(doc.data())
                  });

                  var nameExists = "";
                  var profileNames = "";
                  var profileCount = 0;

                  profiles.forEach((profile, index) => {
                      profileCount += 1;
                      profileNames += profile.name + ". Or ";

                      if ((profile.name).toString() == (paramsNameExists1.name).toString()) {
                          nameExists = (profile.name).toString();
                      }
                  })
                  profileNames = profileNames.slice(0, -4); //remove ' or ' at end

                  if (nameExists == "") {
                      var profileSpeech = "That profile name is not in the database. You have " + profileCount + " profiles. Please choose a profile to add data to from. " + profileNames;

                      response.send({ speech: profileSpeech })
                  } else {
                      var profileSpeech = "What diseases or allergies do you want to add to " + nameExists + "'s profile?";

                      response.send({ speech: profileSpeech })
                  }

              }).catch((e => {
                  console.log("error: went wrong : ", e);
                  response.send({ speech: "Something went wrong: " + e })
              }));



          break;

      case 'checkDataToBeAdded':


          console.log("request.body.result.parameters: ", request.body.result.parameters);
          let paramsNameExists2 = request.body.result.parameters;

          firestore.collection('profiles')
              .get() //get profiles from firestore
              .then(function (querySnapshot) {
                  var profiles = [];
                  querySnapshot.forEach(function (doc) {
                      profiles.push(doc.data())
                  });

                  var profileNames = "";
                  var nameExists = "";
                  var stringAllergies = "";
                  var stringDiseases = "";
                  var allergies = [];
                  var diseases = [];
                  var profileCount = 0;

                  profiles.forEach((profile, index) => {
                      //if parameter name exists in database
                      if ((profile.name).toString() == (paramsNameExists2.name).toString()) {


                          nameExists = (profile.name).toString();

                          //filter so values are unique
                          allergies = (paramsNameExists2.allergies).filter(function (value, index) { return (paramsNameExists2.allergies).indexOf(value) == index });
                          diseases = (paramsNameExists2.diseases).filter(function (value, index) { return (paramsNameExists2.diseases).indexOf(value) == index });

                          //string data into sentences to be in speech
                          allergies.forEach((allergy, index) => {
                              stringAllergies += allergy + " and .";
                          })
                          diseases.forEach((disease, index) => {
                              stringDiseases += disease + " and .";
                          })

                          //remove ' and .' at the end
                          stringDiseases = stringDiseases.slice(0, -5);
                          stringAllergies = stringAllergies.slice(0, -5);
                      }
                      profileCount += 1;
                      //string into sentence to be in speech
                      profileNames += profile.name + ". Or ";
                  })

                  profileNames = profileNames.slice(0, -4); //remove ' or ' at end


                  //further edit speech
                  if (stringDiseases == "") {
                      stringDiseases = "no diseases"
                  } else { stringDiseases = " diseases of " + stringDiseases; }

                  if (stringAllergies == "") {
                      stringAllergies = "no allergies"
                  } else {
                      stringAllergies = " allergies of " + stringAllergies;
                  }

                  //speech
                  if (profileCount == 0) {
                      response.send({ speech: "You have no profiles to add data to. Say create profile if you want to make a new medical profile." })

                  } else if (nameExists == "") {
                      var profileSpeech = "You have " + profileCount + " profiles. Please choose a profile to add data to. " + profileNames;

                      response.send({ speech: profileSpeech })

                  } else {
                      var profileSpeech = "So you want to add " + stringAllergies + " and " + stringDiseases + " to " + nameExists + "'s profile?";

                      response.send({ speech: profileSpeech })
                  }

              })

          break;

      //ACTION 8 add data to firestore
      case 'addDataToDatabase':
          console.log("request.body.result.parameters: ", request.body.result.parameters);
          let paramsDataToAdd = request.body.result.parameters;

          firestore.collection('profiles')
              .get() //get profiles from firestore
              .then(function (querySnapshot) {
                  var profiles = [];
                  querySnapshot.forEach(function (doc) {
                      profiles.push(doc.data())
                  });


                  var nameExists = "";
                  var allergiesArray = [];
                  var diseasesArray = [];

                  var allergiesArrayParams = paramsDataToAdd.allergies;
                  var diseasesArrayParams = paramsDataToAdd.diseases;

                  profiles.forEach((profile, index) => {
                      //if parameter name exists in database
                      if ((profile.name).toString() == (paramsDataToAdd.name).toString()) {


                          nameExists = (profile.name).toString();

                          //filter so values are unique
                          allergiesArray = (profile.allergies).filter(function (value, index) { return (profile.allergies).indexOf(value) == index });
                          diseasesArray = (profile.diseases).filter(function (value, index) { return (profile.diseases).indexOf(value) == index });

                      }
                  })

                  //remove string values from arrays
                  // allergiesArrayParams.filter(e => e !== 'nothing');
                  //  diseasesArrayParams.filter(e => e !== 'no diseases');

                  var stringAllergies = "";
                  var stringDiseases = "";

                  //get allergies and diseases arrays to update database 
                  allergiesArray.forEach((allergy, index) => {
                      if (allergy == "nothing") {
                          allergiesArray = allergiesArrayParams;
                      } else {

                          //check if parameters says add no allergies 
                          var allergyCount = 0;
                          allergiesArrayParams.forEach((paramsAllergy, index) => {
                              if (paramsAllergy == "nothing") {
                                  allergyCount += 1;
                              }
                          })

                          if (allergyCount > 0) {
                              allergiesArrayParams = [];
                          }

                          //combine parameter allergies array with database allergies array
                          allergiesArray = allergiesArray.concat(allergiesArrayParams);
                          //make values unique
                          allergiesArray = allergiesArray.filter(function (value, index) { return allergiesArray.indexOf(value) == index });
                      }

                  })



                  diseasesArray.forEach((disease, index) => {
                      if (disease == "no diseases") {
                          diseasesArray = paramsDataToAdd.diseases;
                      } else {

                          //check if parameters says add no diseases
                          var diseasesCount = 0;
                          diseasesArrayParams.forEach((paramsDisease, index) => {
                              if (paramsDisease == "no diseases") {
                                  diseasesCount += 1;
                              }
                          })

                          if (diseasesCount > 0) {
                              diseasesArrayParams = [];
                          }

                          //combine initial diseases data with new diseases data and make unique
                          diseasesArray = diseasesArray.concat(diseasesArrayParams);
                          diseasesArray = diseasesArray.filter(function (value, index) { return diseasesArray.indexOf(value) == index });
                      }


                  })


                  //speech
                  allergiesArray.forEach((allergy, index) => {
                      stringAllergies += allergy + " and .";
                  })
                  diseasesArray.forEach((disease, index) => {
                      stringDiseases += disease + " and .";
                  })

                  //remove ' and .' at the end
                  stringDiseases = stringDiseases.slice(0, -5);
                  stringAllergies = stringAllergies.slice(0, -5);


                  //update arrays in profile with specified name
                  var update_query = firestore.collection('profiles')
                      .where('name', '==', (paramsDataToAdd.name).toString());

                  update_query
                      .get()
                      .then(function (querySnapshot) {
                          querySnapshot.forEach(function (doc) {
                              doc.ref.update({
                                  allergies: allergiesArray,
                                  diseases: diseasesArray
                              });
                          });

                          response.send({ speech: "Your profile has been updated successfully. You now have " + stringAllergies + " for allergies, and " + stringDiseases + " for diseases in your profile " + nameExists })

                      }).catch((e => {
                          console.log("error: went wrong : ", e);
                          response.send({ speech: "Something went wrong when writing to the database: " + e })
                      }));




              })

          break;

      //ACTION 8 add data to firestore
      case 'createReminder':
          console.log("request.body.result.parameters: ", request.body.result.parameters);
          let paramsReminderToAdd = request.body.result.parameters;

          every = '0'
          if (paramsReminderToAdd.every == 'every') {
              every = '1'
          }


          firestore.collection('Schedule')//get profiles from firestore
              .add({
                  reminderName: paramsReminderToAdd.reminderName,
                  reminderDate: paramsReminderToAdd.reminderDate,
                  reminderTime: paramsReminderToAdd.reminderTime,
                  every: every,
              })
              .then(() => {
                  response.send({ speech: "Your reminder to. " + paramsReminderToAdd.reminderName + ". has been created!" })

              }).catch((e => {
                  console.log("error: went wrong : ", e);
                  response.send({ speech: "something went wrong when writing to the database" })
              }));


          break;

      case 'checkMedicineToDelete':
          firestore.collection('profiles') //get profiles from firestore
              .get() //get profiles from firestore
              .then(function (querySnapshot) {
                  var profiles = [];
                  querySnapshot.forEach(function (doc) {
                      profiles.push(doc.data())
                  });
                  var profileNames = "";

                  var profileCount = 0;
                  profiles.forEach((profile, index) => {
                      profileCount += 1;
                      profileNames += profile.name + ". Or ";
                  })
                  profileNames = profileNames.slice(0, -4); //remove ' or ' at end

                  if (profileCount == 0) {
                      response.send({ speech: "You have no profiles to delete. Say create profile if you want to make a new medical profile." })
                  } else {
                      var profileSpeech = "You have " + profileCount + " profiles. Please choose a profile to delete. " + profileNames;

                      response.send({ speech: profileSpeech })
                  }



              })
          break;
      default:

  }

});
