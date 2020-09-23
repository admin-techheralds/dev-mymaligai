
showErrorAlert = function(msg) {
    $(document).Toasts('create', {
        title:'Error',
        class: 'bg-danger',
        autohide : true,
        delay : 2000,
        body : msg
    })
}

showElement = function(e) {
    $(e).show();
}
hideElement = function(e) {
    $(e).hide();
}
