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

    var taskModal = document.getElementById('taskModal');
    if (taskModal) {
        taskModal.addEventListener('show.bs.modal', function(event) {
            var button = event.relatedTarget;
            var taskId = button.getAttribute('data-task-id');
            var modalBody = document.getElementById('taskModalBody');
            
            modalBody.innerHTML = `
                <div class="text-center p-4">
                    <div class="spinner-border text-primary" role="status">
                        <span class="visually-hidden">Загрузка...</span>
                    </div>
                </div>
            `;
            
            fetch('/tasks/' + taskId + '/details')
                .then(response => response.text())
                .then(html => {
                    modalBody.innerHTML = html;
                })
                .catch(error => {
                    modalBody.innerHTML = '<div class="alert alert-danger">Ошибка загрузки деталей задачи</div>';
                });
        });
    }
});