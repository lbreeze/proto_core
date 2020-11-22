package ru.v6.mark.prototype.domain.constant;

public enum AperakStatus implements EnumDesc {

    OK("•Выполнена полная приемка, товары в полном объеме"),
    NOT_OK("•Выполнена частичная приемка, часть товаров принята с количеством 0 шт"),
    ERR("•Документ не прошел проверку ");

    private String description;

    AperakStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
