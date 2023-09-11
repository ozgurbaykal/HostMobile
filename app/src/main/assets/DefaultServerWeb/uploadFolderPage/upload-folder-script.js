function handleFolderSelection(event) {
    const files = event.target.files;
    const folderData = new FormData();

    // Klasör ismini elde edelim (ilk dosyanın adından elde edebiliriz)
const folderName = files[0].webkitRelativePath.split("/")[0];
folderData.append("folderName", folderName); // Bu satırı ekleyin.
console.log("Folder Name:", folderName);

for (let i = 0; i < files.length; i++) {
    const relativePath = files[i].webkitRelativePath;
    folderData.append("filePaths[]", relativePath);
    folderData.append("files[]", files[i]);
}

    var httpLink = "http://" + location.host + "/postWebFolders";
fetch(httpLink, {
    method: 'POST',
    body: folderData
})
.then(response => {
    if (response.ok) {
        Swal.fire('Success!', 'Your folder successfully uploaded to app.', 'success');
    }
    return response.json();
})
.then(data => {
    console.log(data.message + " ERROR CODE: " + data.error_code);

    if(data.error_code == 21001){
        Swal.fire('Error!', 'Server cant find folder name in your request, please try again.', 'error');
    }else if(data.error_code == 21002){
        Swal.fire('Error!', 'Failed to create directory on server side, please try again.', 'error');
    }else if(data.error_code == 21003){
             Swal.fire('Error!', 'Internal server error, please try again.', 'error');
    }
})
.catch(error => {
    console.error("Error fetching: ", error);
});

}

function uploadFolders() {
    document.getElementById("folderUploader").click();
}
