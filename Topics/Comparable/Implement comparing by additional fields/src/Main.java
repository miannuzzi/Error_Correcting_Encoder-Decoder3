class Article implements Comparable<Article> {
    private String title;
    private int size;

    public Article(String title, int size) {
        this.title = title;
        this.size = size;
    }

    public String getTitle() {
        return this.title;
    }

    public int getSize() {
        return this.size;
    }

    @Override
    public int compareTo(Article otherArticle) {
        // add your code here!
        int result = 0;

        if (this.getSize() < otherArticle.getSize()) {
            result = -1;
        } else if (this.getSize() == otherArticle.getSize()) {
            result = this.getTitle().compareTo(otherArticle.getTitle());
        } else {
            result = 1;
        }

        return result;
    }
}