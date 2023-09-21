var cardValue;

//card value must be;
/*
writeStringCard
writeIntegerCard
writeBooleanCard
removeKeyCard
readStringCard
readIntegerCard
readBooleanCard
removeAllCard
*/

const sharedPrefInput1Text = document.getElementById("sharedPrefInput1Text");
const sharedPrefInput2Text = document.getElementById("sharedPrefInput2Text");
const sharedPrefInput1 = document.getElementById("sharedPrefInput1");
const sharedPrefInput2 = document.getElementById("sharedPrefInput2");
const sharedPrefSendButton = document.getElementById("sharedPrefSendButton");
const removePrefSendButton = document.getElementById("removePrefSendButton");
const removePrefInput1 = document.getElementById("removePrefInput1");

$("#writeStringCard").click(function () {
    cardValue = this.id.toString()

    sharedPrefInput1Text.innerText = "String Pref Key"
    sharedPrefInput2Text.innerText = "String Value"

    openPreferenceModal();
});

$("#writeIntegerCard").click(function () {
    cardValue = this.id.toString()

    sharedPrefInput1Text.innerText = "Integer Pref Key"
    sharedPrefInput2Text.innerText = "Integer Value"

    openPreferenceModal();
});

$("#writeBooleanCard").click(function () {
    cardValue = this.id.toString()

    sharedPrefInput1Text.innerText = "Boolean Pref Key"
    sharedPrefInput2Text.innerText = "Boolean Value"

    openPreferenceModal();
});

$("#removeKeyCard").click(function () {
    cardValue = this.id.toString()

    openRemovePreferenceModal();
});

$("#readStringCard").click(function () {
    cardValue = this.id.toString()

    sharedPrefInput1Text.innerText = "String Pref Key"
    sharedPrefInput2Text.innerText = "String Default Value"
    sharedPrefInput2.placeholder = "Enter Default Value"

    openPreferenceModal();
});

$("#readIntegerCard").click(function () {
    cardValue = this.id.toString()

    sharedPrefInput1Text.innerText = "Integer Pref Key"
    sharedPrefInput2Text.innerText = "Integer Default Value"
    sharedPrefInput2.placeholder = "Enter Default Value"

    openPreferenceModal();
});

$("#readBooleanCard").click(function () {
    cardValue = this.id.toString()

    sharedPrefInput1Text.innerText = "Boolean Pref Key"
    sharedPrefInput2Text.innerText = "Boolean Default Value"
    sharedPrefInput2.placeholder = "Enter Default Value"

    openPreferenceModal();
});

$("#removeAllCard").click(function () {
    cardValue = this.id.toString()

    Swal.fire({
        title: 'Are you sure to delete the entire preference file?',
        showDenyButton: true,
        confirmButtonText: 'Yes',
        denyButtonText: 'No',
        customClass: {
          actions: 'my-actions',
          cancelButton: 'order-1 right-gap',
          confirmButton: 'order-2',
          denyButton: 'order-3',
        }
      }).then((result) => {
        if (result.isConfirmed) {

            httpLink = `http://${location.host}/sharedpreference/remove/all`;

            fetch(httpLink, {
                method: 'DELETE',
                credentials: 'include'
            })
            .then(response => {

                return response.json();
            })
            .then(data => {
                console.log(data)
                if (data.success) {
                   Swal.fire('Success!', 'Your ALL preference deleted successfully', 'success');
                }else{
                   Swal.fire('Error!  ', "INTERNAL SERVER ERROR", 'error');
                }

            })
            .catch(error => {
                console.error('error:', error);
                Swal.fire('Error!  ' + "Something happened this side.", 'error');
            });

          Swal.fire('Saved!', '', 'success')
        } else if (result.isDenied) {
          Swal.fire('The transaction has been cancelled.', '', 'info')
        }
      })

});

sharedPrefSendButton.onclick = function () {
    let checkPrefInputValue = checkPrefDialogInputs();

    if (checkPrefInputValue != true) {
        Swal.fire(
            'Empty Fields!',
            'Please fill in all fields.',
            'error'
        )
    } else {
        if (cardValue == "writeStringCard" || cardValue == "writeIntegerCard" || cardValue == "writeBooleanCard") {

            var httpLink;

            if (cardValue == "writeStringCard")
                httpLink = "http://" + location.host + "/sharedpreference/write/string";
            else if (cardValue == "writeIntegerCard")
                httpLink = "http://" + location.host + "/sharedpreference/write/int";
            else if (cardValue == "writeBooleanCard")
                httpLink = "http://" + location.host + "/sharedpreference/write/boolean";

            const data = {
                key: sharedPrefInput1.value.toString(),
                value: sharedPrefInput2.value.toString()
            };

            fetch(httpLink, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(data)
            })
                .then(response => {
                    if (response.ok) {
                       Swal.fire('Success!', 'Your preference saved successfully', 'success');
                           $('#preferenceModal').modal('hide');
                    }
                    return response.json();
                })
                .then(data => {
                    console.log(data);

                        if(data.error_code == 41001 || data.error_code == 61001 || data.error_code == 81001){
                            Swal.fire('Error!  ' + data.error_code, data.message, 'error');
                        }else if(data.error_code == 41002 || data.error_code == 61002 || data.error_code == 81002){
                            Swal.fire('Error!  ' + data.error_code, data.message, 'error');
                        }else if(data.error_code == 41003 || data.error_code == 61003 || data.error_code == 81003){
                            Swal.fire('Error!  ' + data.error_code, data.message, 'error');
                        }

                })
                .catch(error => {
                    console.error('error:', error);
                    Swal.fire('Error!  ' + "Something happened this side.", 'error');
                });

        } else if (cardValue == "readStringCard" || cardValue == "readIntegerCard" || cardValue == "readBooleanCard") {

            const key = sharedPrefInput1.value.toString();
            const defaultString = sharedPrefInput2.value.toString();

            console.log(key + "  /  " + defaultString)


            if (cardValue == "readStringCard")
            httpLink = `http://${location.host}/sharedpreference/read/string?key=${key}&defaultValue=${defaultString}`;
            else if (cardValue == "readIntegerCard")
            httpLink = `http://${location.host}/sharedpreference/read/int?key=${key}&defaultValue=${defaultString}`;
            else if (cardValue == "readBooleanCard")
            httpLink = `http://${location.host}/sharedpreference/read/boolean?key=${key}&defaultValue=${defaultString}`;

            fetch(httpLink)
                .then(response => response.json())
                .then(data => {
                    console.log(data)
                    if (data.success) {
                        Swal.fire('Success!', 'Your Pref Value: ' + data.message, 'success');
                    } else {
                      Swal.fire('Error!  ' + data.error_code, data.message, 'error');
                    }
                })
                .catch(error => {
                    console.error('error:', error);
                    Swal.fire('Error!  ' + "Something happened this side.", 'error');
                });


        }
    }


};


removePrefSendButton.onclick = function () {
            const key = removePrefInput1.value.toString();

            httpLink = `http://${location.host}/sharedpreference/remove?key=${key}`;

                fetch(httpLink, {
                    method: 'DELETE',
                    credentials: 'include'
                })
                .then(response => {

                    return response.json();
                })
                .then(data => {
                    console.log(data)
                    if (data.success) {
                       Swal.fire('Success!', 'Your preference deleted successfully', 'success');
                           $('#removePrefModal').modal('hide');
                    }else{
                       Swal.fire('Error!  ' + data.error_code, data.message, 'error');
                    }

                })
                .catch(error => {
                    console.error('error:', error);
                    Swal.fire('Error!  ' + "Something happened this side.", 'error');
                });
}

function checkPrefDialogInputs() {

    if (sharedPrefInput1.value != "" || sharedPrefInput2.value != "")
        return true
    else
        return false
}

function openPreferenceModal() {
    sharedPrefInput1.value = ""
    sharedPrefInput2.value = ""

    $('#preferenceModal').modal('show');
}

function openRemovePreferenceModal() {
    removePrefInput1.value = ""

    $('#removePrefModal').modal('show');
}