document.addEventListener('DOMContentLoaded', function() {
    var modal = document.getElementById("evalModal");
    if (modal){
        modal.addEventListener('show.bs.modal', function (event) {
        var button = event.relatedTarget;
        var taskId = button.getAttribute('data-task-id');
        
        var csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');

        var form = this.querySelector('form');
        if (form) {
            form.action = '/tasks/' + taskId + '/review';
        } else {
            console.error('Форма не найдена внутри модалки');
        }
        var csrfInput = document.createElement('input');
        csrfInput.type = 'hidden';
        csrfInput.name = '_csrf';
        csrfInput.value = csrfToken;
        form.appendChild(csrfInput);
        });
    }
});