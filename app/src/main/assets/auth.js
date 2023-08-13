document.addEventListener("DOMContentLoaded", function() {
    const inputs = document.querySelectorAll("form input[type='password']");

    inputs.forEach((input, index) => {
        input.addEventListener("input", function(e) {
           
            if (this.value.length === 1) {
                
                if (index < inputs.length - 1) {
                    inputs[index + 1].focus();
                }
            }
        });

        input.addEventListener("keydown", function(e) {
            if ((e.keyCode === 8 || e.keyCode === 46) && this.value.length === 0) {
                if (index > 0) {
                    e.preventDefault();
                    inputs[index - 1].focus();
                }
            }
        });

        // Eğer son inputta ise ve Enter tuşuna basıldıysa loginClick() fonksiyonunu çalıştır
        if (index === inputs.length - 1) {
            input.addEventListener("keyup", function(e) {
                if (e.keyCode === 13) {  // 13 numaralı kod Enter tuşunun kodudur.
                    loginClick();
                }
            });
        }
    });
});

async function sha256(message) {
    var hashHex = CryptoJS.SHA256(message).toString();
    return hashHex;
}

async function loginClick(){
    const inputs = document.querySelectorAll("form input[type='password']");
    let password = "";

    inputs.forEach(input => {
        password += input.value;
    });

    const encryptedPassword = await sha256(password);

    var httpLink = "http://" + location.host + "/postAuthPassword";


   Swal.fire({
        title: 'Verifying...',
        text: 'Please Wait.',
        didOpen: () => {
            Swal.showLoading()
        },
        allowOutsideClick: false,
        timer: 10000,
        timerProgressBar: true
    });


    fetch(httpLink, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            password: encryptedPassword
        })
    })
    .then(response => response.json())
    .then(data => {
        Swal.close();

        if(data.password_status === true) {
            Swal.fire('Success!', 'Password Correct', 'success');
            location.reload();
        } else if(data.password_status === false) {
            Swal.fire('Error!', 'Wrong Password.', 'error');
        } else {
            Swal.fire('Error!', 'Somethings happen please try again.', 'error');
        }
    })
    .catch(error => {
        Swal.close();
        Swal.fire('Error!', 'Server not responding, check server (mobile app) and try again.', 'error');
    });
}
