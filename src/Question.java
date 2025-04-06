public class Question {

    private String question;
    private String[] answers;
    private int correctAnswer;
    private int questionNumber;

    public Question(String question, String[] answers, int correctAnswer, int questionNumber){
        question = this.question;
        answers = this.answers;
        correctAnswer = this.correctAnswer;
        questionNumber = this.questionNumber;
    }

    public String getQuestion(){
        return question;
    }
    public String[] getAnswers() {
        return answers;
    }
    //returns the correct answer at a specific value in 
    public String getCorrectAnswer(int i){
        return answers[i];
    }
    public int getQuestionNumber(){
        return questionNumber;
    }
}
