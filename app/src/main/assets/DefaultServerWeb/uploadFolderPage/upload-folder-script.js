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
.then(response => response.json())
.then(data => {
    console.log(data.message);
})
.catch(error => {
    console.error("Error fetching: ", error);
});

}

function uploadFolders() {
    document.getElementById("folderUploader").click();
}
